package com.mywallet.wallet.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
public class WalletCreditWriter {

	static final UUID SYSTEM_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final WalletAccountRepository accounts;
	private final LedgerEntryRepository ledger;
	private final WalletLimitRepository limits;
	private final Clock clock;

	public WalletCreditWriter(
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
	public MovementResponse apply(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserIdForUpdate(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TOPUP)
				.orElse(null);
		if (existing != null) {
			return same(existing, amount);
		}
		if (wallet.getStatus() != WalletStatus.ACTIVE) {
			return MovementResponse.rejected(wallet.getStatus() == WalletStatus.FROZEN ? "WALLET_FROZEN" : "WALLET_CLOSED");
		}
		WalletLimit limit = limits.findById(OperationType.TOPUP).orElseThrow(() -> new IllegalStateException("limite"));
		if (amount.compareTo(limit.getMinAmount()) < 0 || amount.compareTo(limit.getMaxAmount()) > 0) {
			return MovementResponse.rejected("LIMIT_EXCEEDED");
		}
		BigDecimal already = ledger.sumSince(wallet.getId(), EntryType.TOPUP, Direction.CREDIT, startOfUtcDay());
		if (already == null) {
			already = BigDecimal.ZERO;
		}
		if (already.add(amount).compareTo(limit.getDailyMax()) > 0) {
			return MovementResponse.rejected("LIMIT_EXCEEDED");
		}
		WalletAccount system = accounts.findByIdForUpdate(SYSTEM_ACCOUNT_ID)
				.orElseThrow(() -> new IllegalStateException("compte système"));
		Instant now = clock.instant();
		BigDecimal balanceAfter = wallet.credit(amount);
		BigDecimal systemAfter = system.debit(amount);
		ledger.save(LedgerEntry.topupCredit(UUID.randomUUID(), wallet.getId(), transactionRef, amount, balanceAfter, now));
		ledger.save(LedgerEntry.topupDebit(UUID.randomUUID(), system.getId(), transactionRef, amount, systemAfter, now));
		return MovementResponse.applied(balanceAfter);
	}

	@Transactional(readOnly = true)
	public MovementResponse replay(UUID userId, String transactionRef, BigDecimal amount) {
		WalletAccount wallet = accounts.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.TOPUP)
				.orElseThrow(() -> new IllegalStateException("écriture"));
		return same(existing, amount);
	}

	private static MovementResponse same(LedgerEntry existing, BigDecimal amount) {
		if (existing.getAmount().compareTo(amount) != 0) {
			throw new IdempotencyConflictException();
		}
		return MovementResponse.applied(existing.getBalanceAfter());
	}

	private Instant startOfUtcDay() {
		return clock.instant().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	public static boolean ledgerConflict(DataIntegrityViolationException exception) {
		String name = DataIntegrityConstraints.name(exception);
		return name != null && name.contains("transaction_ref");
	}

}
