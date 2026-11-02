package com.mywallet.wallet.exception;

public class WalletAlreadyExistsException extends RuntimeException {

	public WalletAlreadyExistsException() {
		super("Un wallet existe déjà.");
	}

}
