package com.mywallet.wallet.exception;

public class ForbiddenException extends RuntimeException {

	public ForbiddenException() {
		super("Accès refusé.");
	}

}
