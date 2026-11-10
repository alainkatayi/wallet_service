package com.mywallet.wallet.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.wallet.dto.MovementResponse;
import com.mywallet.wallet.entity.Direction;
import com.mywallet.wallet.entity.EntryType;
import com.mywallet.wallet.entity.LedgerEntry;
import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.exception.WalletNotFoundException;
import com.mywallet.wallet.repository.LedgerEntryRepository;
import com.mywallet.wallet.repository.WalletAccountRepository;

@Service
public class WalletReverseWriter {

	private final WalletAccountRepository accounts;
	private final LedgerEntryRepository ledger;
	private final Clock clock;

	public WalletReverseWriter(WalletAccountRepository accounts, LedgerEntryRepository ledger, Clock clock) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.clock = clock;
	}

	@Transactional
	public MovementResponse apply(UUID userId, String transactionRef) {
		WalletAccount wallet = accounts.findByUserIdForUpdate(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry already = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.REVERSAL)
				.orElse(null);
		if (already != null) {
			return MovementResponse.applied(already.getBalanceAfter());
		}
		LedgerEntry debit = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.WITHDRAW)
				.orElse(null);
		if (debit == null || debit.getDirection() != Direction.DEBIT) {
			return MovementResponse.rejected("NOTHING_TO_REVERSE");
		}
		WalletAccount system = accounts.findByIdForUpdate(WalletCreditWriter.SYSTEM_ACCOUNT_ID)
				.orElseThrow(() -> new IllegalStateException("compte système"));
		Instant now = clock.instant();
		var balanceAfter = wallet.credit(debit.getAmount());
		var systemAfter = system.debit(debit.getAmount());
		ledger.save(LedgerEntry.reversal(
				UUID.randomUUID(), wallet.getId(), transactionRef, Direction.CREDIT, debit.getAmount(), balanceAfter, now));
		ledger.save(LedgerEntry.reversal(
				UUID.randomUUID(), system.getId(), transactionRef, Direction.DEBIT, debit.getAmount(), systemAfter, now));
		return MovementResponse.applied(balanceAfter);
	}

	@Transactional(readOnly = true)
	public MovementResponse replay(UUID userId, String transactionRef) {
		WalletAccount wallet = accounts.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
		LedgerEntry existing = ledger
				.findByAccountIdAndTransactionRefAndEntryType(wallet.getId(), transactionRef, EntryType.REVERSAL)
				.orElseThrow(() -> new IllegalStateException("écriture"));
		return MovementResponse.applied(existing.getBalanceAfter());
	}

}
