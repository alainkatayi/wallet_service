package com.mywallet.wallet.exception;

public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException() {
		super("La requête est invalide.");
	}

}
