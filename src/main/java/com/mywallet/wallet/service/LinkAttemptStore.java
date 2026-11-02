package com.mywallet.wallet.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mywallet.wallet.entity.WalletLinkAttempt;
import com.mywallet.wallet.exception.TooManyRequestsException;
import com.mywallet.wallet.repository.WalletLinkAttemptRepository;

import jakarta.persistence.EntityManager;

@Component
@Transactional
public class LinkAttemptStore {

	static final int MAX_PER_HOUR = 5;

	private final WalletLinkAttemptRepository attempts;
	private final EntityManager entityManager;
	private final Clock clock;

	public LinkAttemptStore(
			WalletLinkAttemptRepository attempts,
			EntityManager entityManager,
			Clock clock) {
		this.attempts = attempts;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Optional<WalletLinkAttempt> find(UUID linkId) {
		return attempts.findById(linkId);
	}

	@Transactional(readOnly = true)
	public void rejectIfLimited(UUID userId) {
		ensureAllowed(userId);
	}

	public void save(UUID linkId, UUID userId, String accountNumber) {
		lock(userId);
		ensureAllowed(userId);
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		attempts.saveAndFlush(WalletLinkAttempt.newAttempt(linkId, userId, accountNumber, now));
	}

	public void delete(UUID linkId) {
		if (attempts.existsById(linkId)) {
			attempts.deleteById(linkId);
			attempts.flush();
		}
	}

	private void ensureAllowed(UUID userId) {
		long retryAfter = retryAfterSeconds(userId);
		if (retryAfter > 0) {
			throw new TooManyRequestsException(retryAfter);
		}
	}

	private long retryAfterSeconds(UUID userId) {
		Instant now = clock.instant();
		Instant since = now.minus(Duration.ofHours(1));
		if (attempts.countByUserIdAndCreatedAtGreaterThanEqual(userId, since) < MAX_PER_HOUR) {
			return 0;
		}
		Instant oldest = attempts
				.findFirstByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(userId, since)
				.map(WalletLinkAttempt::getCreatedAt)
				.orElse(null);
		if (oldest == null) {
			return 3600;
		}
		long seconds = Duration.between(now, oldest.plus(Duration.ofHours(1))).toSeconds();
		return Math.max(seconds, 1);
	}

	private void lock(UUID userId) {
		// La fonction renvoie void : on l'enveloppe pour que JDBC voie un entier, pas un type inconnu.
		entityManager.createNativeQuery(
				"SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(:userId, 0))) AS acquired")
				.setParameter("userId", userId.toString())
				.getSingleResult();
	}

}
