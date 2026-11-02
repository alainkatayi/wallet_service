package com.mywallet.wallet.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * link_id est imposé (celui de la banque). Sans Persistable, Spring Data prend la ligne pour existante et fait un merge.
 */
@Entity
@Table(name = "wallet_link_attempt")
public class WalletLinkAttempt implements Persistable<UUID> {

	@Id
	@Column(name = "link_id", nullable = false, updatable = false)
	private UUID linkId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "account_number", nullable = false, length = 12, updatable = false)
	private String accountNumber;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean newEntity;

	protected WalletLinkAttempt() {
		this.newEntity = false;
	}

	private WalletLinkAttempt(UUID linkId, UUID userId, String accountNumber, Instant createdAt) {
		this.linkId = linkId;
		this.userId = userId;
		this.accountNumber = accountNumber;
		this.createdAt = createdAt;
		this.newEntity = true;
	}

	public static WalletLinkAttempt newAttempt(UUID linkId, UUID userId, String accountNumber, Instant createdAt) {
		return new WalletLinkAttempt(linkId, userId, accountNumber, createdAt);
	}

	@Override
	public UUID getId() {
		return linkId;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.newEntity = false;
	}

	public UUID getLinkId() {
		return linkId;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
