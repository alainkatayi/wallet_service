package com.mywallet.wallet.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mywallet.wallet.dto.WalletResponse;
import com.mywallet.wallet.security.CurrentUser;
import com.mywallet.wallet.service.WalletOpen;
import com.mywallet.wallet.service.WalletService;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

	private final CurrentUser currentUser;
	private final WalletService walletService;

	public WalletController(CurrentUser currentUser, WalletService walletService) {
		this.currentUser = currentUser;
		this.walletService = walletService;
	}

	@PostMapping("/me")
	public ResponseEntity<WalletResponse> create() {
		WalletOpen open = walletService.open(currentUser.id());
		WalletResponse body = WalletResponse.from(open.account());
		if (open.created()) {
			return ResponseEntity.created(URI.create("/api/wallets/me")).body(body);
		}
		return ResponseEntity.ok(body);
	}

	@GetMapping("/me")
	public WalletResponse me() {
		return WalletResponse.from(walletService.get(currentUser.id()));
	}

}
