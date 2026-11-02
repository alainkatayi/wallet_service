package com.mywallet.wallet.service;

import com.mywallet.wallet.entity.WalletAccount;

public record WalletOpen(WalletAccount account, boolean created) {
}
