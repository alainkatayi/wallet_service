package com.mywallet.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Un identifiant assigné avec @Version fait croire à Spring Data que la ligne existe déjà, donc save() part en merge et n'insère rien.
 * Persistable garde newEntity à vrai jusqu'à @PostPersist et @PostLoad, pour que saveAndFlush exécute un persist.
 */
@Entity
@Table(name = "wallet_account")
public class WalletAccount implements Persistable<UUID> {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 10, updatable = false)
	private AccountKind kind;

	@Column(name = "user_id", unique = true, updatable = false)
	private UUID userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "bank_account_number", unique = true, length = 12, updatable = false)
	private String bankAccountNumber;

	@Column(name = "balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "currency", nullable = false, length = 3, updatable = false)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private WalletStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean newEntity;

	protected WalletAccount() {
		this.newEntity = false;
	}

	private WalletAccount(UUID id, UUID userId, String bankAccountNumber, Instant now) {
		this.id = id;
		this.kind = AccountKind.USER;
		this.userId = userId;
		this.bankAccountNumber = bankAccountNumber;
		this.balance = new BigDecimal("0.00");
		this.currency = "CDF";
		this.status = WalletStatus.ACTIVE;
		this.createdAt = now;
		this.newEntity = true;
	}

	public static WalletAccount newUserWallet(UUID id, UUID userId, String bankAccountNumber, Instant now) {
		return new WalletAccount(id, userId, bankAccountNumber, now);
	}

	@Override
	public UUID getId() {
		return id;
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

	public AccountKind getKind() {
		return kind;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getBankAccountNumber() {
		return bankAccountNumber;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public String getCurrency() {
		return currency;
	}

	public WalletStatus getStatus() {
		return status;
	}

	public Long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
