package com.mywallet.wallet.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.entity.WalletStatus;

public record WalletResponse(
		UUID walletId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
		String currency,
		WalletStatus status,
		Instant createdAt,
		String accountNumber) {

	public static WalletResponse from(WalletAccount account) {
		return new WalletResponse(
				account.getId(),
				account.getBalance().setScale(2, RoundingMode.UNNECESSARY),
				account.getCurrency().trim(),
				account.getStatus(),
				account.getCreatedAt(),
				mask(account.getBankAccountNumber()));
	}

	private static String mask(String accountNumber) {
		if (accountNumber == null) {
			return null;
		}
		String digits = accountNumber.trim();
		if (digits.length() <= 4) {
			return digits;
		}
		return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
	}

}
