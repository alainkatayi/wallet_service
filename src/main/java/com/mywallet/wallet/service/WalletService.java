package com.mywallet.wallet.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.exception.WalletNotFoundException;

@Service
public class WalletService {

	private final WalletCreator walletCreator;

	public WalletService(WalletCreator walletCreator) {
		this.walletCreator = walletCreator;
	}

	public WalletAccount get(UUID userId) {
		return walletCreator.findUserWallet(userId).orElseThrow(WalletNotFoundException::new);
	}

}
