package com.mywallet.wallet.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.mywallet.wallet.config.InternalApiKey;
import com.mywallet.wallet.dto.InternalWalletResponse;
import com.mywallet.wallet.dto.MovementResponse;
import com.mywallet.wallet.exception.WalletNotFoundException;
import com.mywallet.wallet.service.WalletCreditService;
import com.mywallet.wallet.service.WalletService;

import tools.jackson.databind.JsonNode;

@RestController
public class InternalWalletController {

	private final InternalApiKey apiKey;
	private final WalletService wallets;
	private final WalletCreditService credits;

	public InternalWalletController(InternalApiKey apiKey, WalletService wallets, WalletCreditService credits) {
		this.apiKey = apiKey;
		this.wallets = wallets;
		this.credits = credits;
	}

	@GetMapping("/internal/wallets/{userId}")
	public InternalWalletResponse get(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key) {
		apiKey.check(key);
		return InternalWalletResponse.from(wallets.get(userId(userId)));
	}

	@PostMapping("/internal/wallets/{userId}/credit")
	public MovementResponse credit(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return credits.credit(userId(userId), body);
	}

	private static UUID userId(String userId) {
		try {
			return UUID.fromString(userId);
		}
		catch (IllegalArgumentException exception) {
			throw new WalletNotFoundException();
		}
	}

}
