package com.mywallet.wallet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mywallet.wallet.entity.AccountKind;
import com.mywallet.wallet.entity.WalletAccount;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {

	Optional<WalletAccount> findByUserId(UUID userId);

	List<WalletAccount> findByKind(AccountKind kind);

}
