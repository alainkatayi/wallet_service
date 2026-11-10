package com.mywallet.wallet.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.wallet.dto.MovementResponse;
import com.mywallet.wallet.entity.Direction;
import com.mywallet.wallet.entity.EntryType;
import com.mywallet.wallet.entity.LedgerEntry;
import com.mywallet.wallet.entity.OperationType;
import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.entity.WalletLimit;
import com.mywallet.wallet.entity.WalletStatus;
import com.mywallet.wallet.exception.IdempotencyConflictException;
import com.mywallet.wallet.exception.WalletNotFoundException;
import com.mywallet.wallet.repository.LedgerEntryRepository;
import com.mywallet.wallet.repository.WalletAccountRepository;
import com.mywallet.wallet.repository.WalletLimitRepository;

@Service
public class WalletTransferWriter {

	private final WalletAccountRepository accounts;
	private final LedgerEntryRepository ledger;
	private final WalletLimitRepository limits;
	private final Clock clock;

	public WalletTransferWriter(
			WalletAccountRepository accounts,
			LedgerEntryRepository ledger,
			WalletLimitRepository limits,
			Clock clock) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.limits = limits;
		this.clock = clock;
	}

	@Transactional
	public MovementResponse debit(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserIdForUpdate(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TRANSFER)
				.orElse(null);
		if (existing != null) {
			return sameDebit(existing, amount);
		}
		if (wallet.getStatus() != WalletStatus.ACTIVE) {
			return MovementResponse.rejected(wallet.getStatus() == WalletStatus.FROZEN ? "WALLET_FROZEN" : "WALLET_CLOSED");
		}
		WalletLimit limit = limits.findById(OperationType.TRANSFER).orElseThrow(() -> new IllegalStateException("limite"));
		if (amount.compareTo(limit.getMinAmount()) < 0 || amount.compareTo(limit.getMaxAmount()) > 0) {
			return MovementResponse.rejected("LIMIT_EXCEEDED");
		}
		BigDecimal already = ledger.sumSince(wallet.getId(), EntryType.TRANSFER, Direction.DEBIT, startOfUtcDay());
		if (already == null) {
			already = BigDecimal.ZERO;
		}
		if (already.add(amount).compareTo(limit.getDailyMax()) > 0) {
			return MovementResponse.rejected("LIMIT_EXCEEDED");
		}
		if (wallet.getBalance().compareTo(amount) < 0) {
			return MovementResponse.rejected("INSUFFICIENT_FUNDS");
		}
		Instant now = clock.instant();
		BigDecimal balanceAfter = wallet.debit(amount);
		ledger.save(LedgerEntry.transferDebit(UUID.randomUUID(), wallet.getId(), transactionRef, amount, balanceAfter, now));
		return MovementResponse.applied(balanceAfter);
	}

	@Transactional(readOnly = true)
	public MovementResponse replayDebit(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TRANSFER)
				.orElseThrow(() -> new IllegalStateException("écriture"));
		return sameDebit(existing, amount);
	}

	@Transactional
	public MovementResponse credit(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserIdForUpdate(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TRANSFER)
				.orElse(null);
		if (existing != null) {
			return sameCredit(existing, amount);
		}
		if (wallet.getStatus() != WalletStatus.ACTIVE) {
			return MovementResponse.rejected(wallet.getStatus() == WalletStatus.FROZEN ? "WALLET_FROZEN" : "WALLET_CLOSED");
		}
		Instant now = clock.instant();
		BigDecimal balanceAfter = wallet.credit(amount);
		ledger.save(LedgerEntry.transferCredit(UUID.randomUUID(), wallet.getId(), transactionRef, amount, balanceAfter, now));
		return MovementResponse.applied(balanceAfter);
	}

	@Transactional(readOnly = true)
	public MovementResponse replayCredit(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TRANSFER)
				.orElseThrow(() -> new IllegalStateException("écriture"));
		return sameCredit(existing, amount);
	}

	@Transactional
	public MovementResponse reverse(UUID userId, String transactionRef) {
		WalletAccount wallet = accounts.findByUserIdForUpdate(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry already = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.REVERSAL)
				.orElse(null);
		if (already != null) {
			return MovementResponse.applied(already.getBalanceAfter());
		}
		LedgerEntry debit = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TRANSFER)
				.orElse(null);
		if (debit == null || debit.getDirection() != Direction.DEBIT) {
			return MovementResponse.rejected("NOTHING_TO_REVERSE");
		}
		Instant now = clock.instant();
		BigDecimal balanceAfter = wallet.credit(debit.getAmount());
		ledger.save(LedgerEntry.reversal(
				UUID.randomUUID(), wallet.getId(), transactionRef, Direction.CREDIT, debit.getAmount(), balanceAfter, now));
		return MovementResponse.applied(balanceAfter);
	}

	@Transactional(readOnly = true)
	public MovementResponse replayReverse(UUID userId, String transactionRef) {
		WalletAccount wallet = accounts.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.REVERSAL)
				.orElseThrow(() -> new IllegalStateException("écriture"));
		return MovementResponse.applied(existing.getBalanceAfter());
	}

	private static MovementResponse sameDebit(LedgerEntry existing, BigDecimal amount) {
		if (existing.getDirection() != Direction.DEBIT || existing.getAmount().compareTo(amount) != 0) {
			throw new IdempotencyConflictException();
		}
		return MovementResponse.applied(existing.getBalanceAfter());
	}

	private static MovementResponse sameCredit(LedgerEntry existing, BigDecimal amount) {
		if (existing.getDirection() != Direction.CREDIT || existing.getAmount().compareTo(amount) != 0) {
			throw new IdempotencyConflictException();
		}
		return MovementResponse.applied(existing.getBalanceAfter());
	}

	private Instant startOfUtcDay() {
		return clock.instant().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
	}

}
