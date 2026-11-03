package com.mywallet.wallet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mywallet.wallet.entity.AccountKind;
import com.mywallet.wallet.entity.WalletAccount;

import jakarta.persistence.LockModeType;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {

	Optional<WalletAccount> findByUserId(UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from WalletAccount w where w.userId = :userId")
	Optional<WalletAccount> findByUserIdForUpdate(@Param("userId") UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from WalletAccount w where w.id = :id")
	Optional<WalletAccount> findByIdForUpdate(@Param("id") UUID id);

	List<WalletAccount> findByKind(AccountKind kind);

	@Query("select count(w) from WalletAccount w where trim(w.bankAccountNumber) = :accountNumber")
	long countByTrimmedBankAccountNumber(@Param("accountNumber") String accountNumber);

}
