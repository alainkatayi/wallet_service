package com.mywallet.wallet.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mywallet.wallet.client.BankLinkClient;
import com.mywallet.wallet.dto.ConfirmLinkRequest;
import com.mywallet.wallet.dto.InitiateLinkRequest;
import com.mywallet.wallet.dto.InitiateLinkResponse;
import com.mywallet.wallet.dto.WalletResponse;
import com.mywallet.wallet.security.CurrentUser;
import com.mywallet.wallet.service.LinkedWallet;
import com.mywallet.wallet.service.WalletLinkService;
import com.mywallet.wallet.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

	private final CurrentUser currentUser;
	private final WalletService walletService;
	private final WalletLinkService walletLinkService;

	public WalletController(
			CurrentUser currentUser,
			WalletService walletService,
			WalletLinkService walletLinkService) {
		this.currentUser = currentUser;
		this.walletService = walletService;
		this.walletLinkService = walletLinkService;
	}

	@PostMapping("/link/initiate")
	public ResponseEntity<InitiateLinkResponse> initiate(@Valid @RequestBody InitiateLinkRequest request) {
		BankLinkClient.Initiation initiation = walletLinkService.initiate(currentUser.id(), request.accountNumber());
		return ResponseEntity.accepted().body(new InitiateLinkResponse(initiation.linkId(), initiation.expiresIn()));
	}

	@PostMapping("/link/confirm")
	public ResponseEntity<WalletResponse> confirm(@Valid @RequestBody ConfirmLinkRequest request) {
		LinkedWallet linked = walletLinkService.confirm(currentUser.id(), request.linkId(), request.code());
		WalletResponse body = WalletResponse.from(linked.account());
		if (linked.created()) {
			return ResponseEntity.created(URI.create("/api/wallets/me")).body(body);
		}
		return ResponseEntity.ok(body);
	}

	@GetMapping("/me")
	public WalletResponse me() {
		return WalletResponse.from(walletService.get(currentUser.id()));
	}

}
