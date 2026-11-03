package com.mywallet.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mywallet.wallet.entity.OperationType;
import com.mywallet.wallet.entity.WalletLimit;

public interface WalletLimitRepository extends JpaRepository<WalletLimit, OperationType> {
}
