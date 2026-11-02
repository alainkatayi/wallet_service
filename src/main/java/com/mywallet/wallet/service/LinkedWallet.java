package com.mywallet.wallet.service;

import com.mywallet.wallet.entity.WalletAccount;

public record LinkedWallet(WalletAccount account, boolean created) {
}
