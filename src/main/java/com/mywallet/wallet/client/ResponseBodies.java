package com.mywallet.wallet.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;

final class ResponseBodies {

	private ResponseBodies() {
	}

	static void drain(ClientHttpResponse response) {
		try (InputStream body = response.getBody()) {
			body.readAllBytes();
		}
		catch (IOException exception) {
			// le corps peut contenir un jeton ou un code : on ne le journalise pas
		}
	}

	static long retryAfterSeconds(ClientHttpResponse response) {
		String header;
		try {
			header = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
		}
		catch (RuntimeException exception) {
			header = null;
		}
		drain(response);
		if (header == null || header.isBlank()) {
			return 3600;
		}
		try {
			long seconds = Long.parseLong(header.trim());
			return seconds < 1 ? 1 : seconds;
		}
		catch (NumberFormatException exception) {
			return 3600;
		}
	}

}
