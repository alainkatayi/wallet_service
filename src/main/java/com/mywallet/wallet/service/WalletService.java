package com.mywallet.wallet.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.exception.WalletNotFoundException;

@Service
public class WalletService {

	private final WalletCreator walletCreator;

	public WalletService(WalletCreator walletCreator) {
		this.walletCreator = walletCreator;
	}

	public WalletOpen open(UUID userId) {
		try {
			return walletCreator.createIfAbsent(userId);
		}
		catch (DataIntegrityViolationException exception) {
			// une transaction qui a rencontré une erreur de base est condamnée au rollback
			return walletCreator.findUserWallet(userId)
					.map(account -> new WalletOpen(account, false))
					.orElseThrow(() -> exception);
		}
	}

	public WalletAccount get(UUID userId) {
		return walletCreator.findUserWallet(userId).orElseThrow(WalletNotFoundException::new);
	}

}
