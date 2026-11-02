package com.mywallet.wallet.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mywallet.wallet.entity.WalletLinkAttempt;

public interface WalletLinkAttemptRepository extends JpaRepository<WalletLinkAttempt, UUID> {

	long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant since);

	Optional<WalletLinkAttempt> findFirstByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
			UUID userId,
			Instant since);

}
