package com.mywallet.wallet.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywallet.wallet.exception.BankUnavailableException;
import com.mywallet.wallet.exception.InvalidLinkCodeException;
import com.mywallet.wallet.exception.InvalidRequestException;
import com.mywallet.wallet.exception.TooManyRequestsException;

@Component
public class BankLinkClient {

	private final RestClient bank;
	private final BankTokenProvider tokenProvider;

	public BankLinkClient(RemoteHttp remoteHttp, BankTokenProvider tokenProvider,
			@Value("${app.bank.base-url}") String baseUrl) {
		this.bank = remoteHttp.client(baseUrl);
		this.tokenProvider = tokenProvider;
	}

	public Initiation initiate(String accountNumber, String phone) {
		try {
			return postInitiate(accountNumber, phone);
		}
		catch (BankUnauthorizedException first) {
			tokenProvider.invalidate();
			try {
				return postInitiate(accountNumber, phone);
			}
			catch (BankUnauthorizedException second) {
				throw new BankUnavailableException();
			}
		}
	}

	/**
	 * Pas de second essai : le code est à usage unique côté banque.
	 */
	public Confirmation confirm(UUID linkId, String code) {
		try {
			Confirmed body = bank.post()
					.uri("/bank/links/confirm")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.token())
					.body(new ConfirmPayload(linkId.toString(), code))
					.retrieve()
					.onStatus(status -> status.isError(), (request, response) -> failConfirm(response))
					.body(Confirmed.class);
			if (body == null || body.verified() == null || body.accountNumber() == null || body.accountNumber().isBlank()) {
				throw new InvalidLinkCodeException();
			}
			return new Confirmation(body.verified(), body.accountNumber());
		}
		catch (RestClientException exception) {
			throw new BankUnavailableException();
		}
	}

	private Initiation postInitiate(String accountNumber, String phone) {
		try {
			Initiated body = bank.post()
					.uri("/bank/links/initiate")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.token())
					.body(new InitiatePayload(accountNumber, phone))
					.retrieve()
					.onStatus(status -> status.isError(), (request, response) -> failInitiate(response))
					.body(Initiated.class);
			if (body == null || body.linkId() == null || body.expiresIn() == null || body.expiresIn() < 1) {
				throw new BankUnavailableException();
			}
			return new Initiation(body.linkId(), body.expiresIn());
		}
		catch (RestClientException exception) {
			throw new BankUnavailableException();
		}
	}

	private void failInitiate(ClientHttpResponse response) {
		int status = status(response);
		if (status == 401) {
			ResponseBodies.drain(response);
			throw new BankUnauthorizedException();
		}
		if (status == 429) {
			throw new TooManyRequestsException(ResponseBodies.retryAfterSeconds(response));
		}
		if (status == 400) {
			ResponseBodies.drain(response);
			throw new InvalidRequestException();
		}
		ResponseBodies.drain(response);
		throw new BankUnavailableException();
	}

	private void failConfirm(ClientHttpResponse response) {
		int status = status(response);
		ResponseBodies.drain(response);
		if (status == 401) {
			tokenProvider.invalidate();
			throw new BankUnavailableException();
		}
		if (status == 400) {
			throw new InvalidLinkCodeException();
		}
		throw new BankUnavailableException();
	}

	private static int status(ClientHttpResponse response) {
		try {
			return response.getStatusCode().value();
		}
		catch (java.io.IOException exception) {
			throw new BankUnavailableException();
		}
	}

	public record Initiation(UUID linkId, int expiresIn) {
	}

	public record Confirmation(boolean verified, String accountNumber) {
	}

	private record InitiatePayload(String accountNumber, String phone) {
	}

	private record ConfirmPayload(String linkId, String code) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Initiated(UUID linkId, Integer expiresIn) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Confirmed(Boolean verified, String accountNumber) {
	}

}
