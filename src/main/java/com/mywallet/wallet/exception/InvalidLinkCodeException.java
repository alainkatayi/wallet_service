package com.mywallet.wallet.exception;

public class InvalidLinkCodeException extends RuntimeException {

	public InvalidLinkCodeException() {
		super("Code de liaison invalide");
	}

}
