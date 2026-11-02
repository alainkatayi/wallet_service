package com.mywallet.wallet.exception;

public class TooManyRequestsException extends RuntimeException {

	private final long retryAfterSeconds;

	public TooManyRequestsException(long retryAfterSeconds) {
		super("Trop de demandes, réessayez plus tard.");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}

}
