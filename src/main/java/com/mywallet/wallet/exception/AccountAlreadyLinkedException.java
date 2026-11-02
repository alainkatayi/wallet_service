package com.mywallet.wallet.exception;

public class AccountAlreadyLinkedException extends RuntimeException {

	public AccountAlreadyLinkedException() {
		super("Ce compte est déjà lié.");
	}

}
