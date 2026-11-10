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
import com.mywallet.wallet.service.WalletDebitService;
import com.mywallet.wallet.service.WalletService;
import com.mywallet.wallet.service.WalletTransferService;

import tools.jackson.databind.JsonNode;

@RestController
public class InternalWalletController {

	private final InternalApiKey apiKey;
	private final WalletService wallets;
	private final WalletCreditService credits;
	private final WalletDebitService debits;
	private final WalletTransferService transfers;

	public InternalWalletController(
			InternalApiKey apiKey,
			WalletService wallets,
			WalletCreditService credits,
			WalletDebitService debits,
			WalletTransferService transfers) {
		this.apiKey = apiKey;
		this.wallets = wallets;
		this.credits = credits;
		this.debits = debits;
		this.transfers = transfers;
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

	@PostMapping("/internal/wallets/{userId}/debit")
	public MovementResponse debit(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return debits.debit(userId(userId), body);
	}

	@PostMapping("/internal/wallets/{userId}/reverse")
	public MovementResponse reverse(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return debits.reverse(userId(userId), body);
	}

	@PostMapping("/internal/wallets/{userId}/transfer/debit")
	public MovementResponse transferDebit(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return transfers.debit(userId(userId), body);
	}

	@PostMapping("/internal/wallets/{userId}/transfer/credit")
	public MovementResponse transferCredit(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return transfers.credit(userId(userId), body);
	}

	@PostMapping("/internal/wallets/{userId}/transfer/reverse")
	public MovementResponse transferReverse(
			@PathVariable String userId,
			@RequestHeader(value = "X-Internal-Key", required = false) String key,
			@RequestBody(required = false) JsonNode body) {
		apiKey.check(key);
		return transfers.reverse(userId(userId), body);
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
