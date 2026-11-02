package com.mywallet.wallet.exception;

public class UserServiceUnavailableException extends RuntimeException {

	public UserServiceUnavailableException() {
		super("Le service utilisateur est indisponible.");
	}

}
