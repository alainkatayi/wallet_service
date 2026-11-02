package com.mywallet.wallet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mywallet.wallet.entity.AccountKind;
import com.mywallet.wallet.entity.WalletAccount;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {

	Optional<WalletAccount> findByUserId(UUID userId);

	List<WalletAccount> findByKind(AccountKind kind);

	@Query("select count(w) from WalletAccount w where trim(w.bankAccountNumber) = :accountNumber")
	long countByTrimmedBankAccountNumber(@Param("accountNumber") String accountNumber);

}
