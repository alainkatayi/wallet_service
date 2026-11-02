package com.mywallet.wallet.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywallet.wallet.exception.UnauthenticatedException;
import com.mywallet.wallet.exception.UserServiceUnavailableException;

/**
 * Transmet le jeton Bearer de l'utilisateur déjà authentifié.
 * Ce jeton vient du SecurityContext, jamais d'un champ de la requête wallet.
 */
@Component
public class UserServiceClient {

	private final RestClient userService;

	public UserServiceClient(RemoteHttp remoteHttp, @Value("${app.user-service.base-url}") String baseUrl) {
		this.userService = remoteHttp.client(baseUrl);
	}

	public String getProfile(String bearerToken) {
		if (bearerToken == null || bearerToken.isBlank()
				|| bearerToken.indexOf('\r') >= 0 || bearerToken.indexOf('\n') >= 0) {
			throw new UnauthenticatedException();
		}
		try {
			Profile profile = userService.get()
					.uri("/api/users/me")
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
					.retrieve()
					.onStatus(status -> status.value() == 401 || status.value() == 403, (request, response) -> {
						ResponseBodies.drain(response);
						throw new UnauthenticatedException();
					})
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new UserServiceUnavailableException();
					})
					.body(Profile.class);
			if (profile == null || profile.phone() == null || profile.phone().isBlank()) {
				throw new UserServiceUnavailableException();
			}
			return profile.phone();
		}
		catch (RestClientException exception) {
			throw new UserServiceUnavailableException();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Profile(String phone) {
	}

}
