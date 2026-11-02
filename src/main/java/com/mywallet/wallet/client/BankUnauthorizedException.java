package com.mywallet.wallet.client;

final class BankUnauthorizedException extends RuntimeException {

	BankUnauthorizedException() {
		super("Jeton banque refusé.");
	}

}
