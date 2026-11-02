package com.mywallet.wallet.exception;

public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException() {
		super("Authentification requise.");
	}

}
