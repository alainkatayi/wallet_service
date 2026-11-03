package com.mywallet.wallet.exception;

public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException() {
		super("Cette référence est déjà utilisée.");
	}

}
