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

@Component
@Transactional
public class WalletCreator {

	private final WalletAccountRepository walletAccountRepository;
	private final Clock clock;

	public WalletCreator(WalletAccountRepository walletAccountRepository, Clock clock) {
		this.walletAccountRepository = walletAccountRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<WalletAccount> findUserWallet(UUID userId) {
		return walletAccountRepository.findByUserId(userId)
				.filter(account -> account.getKind() == AccountKind.USER);
	}

	public WalletOpen createIfAbsent(UUID userId) {
		Optional<WalletAccount> existing = findUserWallet(userId);
		if (existing.isPresent()) {
			return new WalletOpen(existing.get(), false);
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		WalletAccount wallet = WalletAccount.newUserWallet(UUID.randomUUID(), userId, now);
		return new WalletOpen(walletAccountRepository.saveAndFlush(wallet), true);
	}

}
