package com.mywallet.wallet.dto;

import java.util.UUID;

import com.mywallet.wallet.entity.WalletAccount;
import com.mywallet.wallet.entity.WalletStatus;

public record InternalWalletResponse(UUID walletId, WalletStatus status, String bankAccountNumber) {

	public static InternalWalletResponse from(WalletAccount account) {
		String number = account.getBankAccountNumber();
		return new InternalWalletResponse(
				account.getId(),
				account.getStatus(),
				number == null ? null : number.trim());
	}

}
