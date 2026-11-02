package com.mywallet.wallet.exception;

public class WalletNotFoundException extends RuntimeException {

	public WalletNotFoundException() {
		super("Wallet introuvable.");
	}

}
