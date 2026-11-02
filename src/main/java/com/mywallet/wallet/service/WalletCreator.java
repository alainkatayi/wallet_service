package com.mywallet.wallet.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.wallet.entity.AccountKind;
import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.repository.WalletAccountRepository;
import com.mywallet.wallet.repository.WalletLinkAttemptRepository;

@Component
@Transactional
public class WalletCreator {

	private final WalletAccountRepository walletAccountRepository;
	private final WalletLinkAttemptRepository linkAttemptRepository;
	private final Clock clock;

	public WalletCreator(
			WalletAccountRepository walletAccountRepository,
			WalletLinkAttemptRepository linkAttemptRepository,
			Clock clock) {
		this.walletAccountRepository = walletAccountRepository;
		this.linkAttemptRepository = linkAttemptRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<WalletAccount> findUserWallet(UUID userId) {
		return walletAccountRepository.findByUserId(userId)
				.filter(account -> account.getKind() == AccountKind.USER);
	}

	@Transactional(readOnly = true)
	public boolean bankAccountTaken(String accountNumber) {
		return walletAccountRepository.countByTrimmedBankAccountNumber(accountNumber) > 0;
	}

	public WalletAccount createLinked(UUID userId, String accountNumber, UUID linkId) {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		WalletAccount wallet = WalletAccount.newUserWallet(UUID.randomUUID(), userId, accountNumber, now);
		WalletAccount saved = walletAccountRepository.saveAndFlush(wallet);
		linkAttemptRepository.deleteById(linkId);
		linkAttemptRepository.flush();
		return saved;
	}

}
