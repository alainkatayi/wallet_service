package com.mywallet.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

/**
 * L'identifiant est assigné. Sans Persistable, Spring Data prend la ligne pour existante et fait un merge.
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry implements Persistable<UUID> {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "account_id", nullable = false, updatable = false)
	private UUID accountId;

	@Column(name = "transaction_ref", nullable = false, updatable = false, length = 64)
	private String transactionRef;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, updatable = false, length = 10)
	private EntryType entryType;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false, updatable = false, length = 6)
	private Direction direction;

	@Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal balanceAfter;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean newEntity;

	protected LedgerEntry() {
		this.newEntity = false;
	}

	private LedgerEntry(
			UUID id,
			UUID accountId,
			String transactionRef,
			Direction direction,
			BigDecimal amount,
			BigDecimal balanceAfter,
			Instant createdAt) {
		this.id = id;
		this.accountId = accountId;
		this.transactionRef = transactionRef;
		this.entryType = EntryType.TOPUP;
		this.direction = direction;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.createdAt = createdAt;
		this.newEntity = true;
	}

	public static LedgerEntry topupCredit(
			UUID id,
			UUID accountId,
			String transactionRef,
			BigDecimal amount,
			BigDecimal balanceAfter,
			Instant createdAt) {
		return new LedgerEntry(id, accountId, transactionRef, Direction.CREDIT, amount, balanceAfter, createdAt);
	}

	public static LedgerEntry topupDebit(
			UUID id,
			UUID accountId,
			String transactionRef,
			BigDecimal amount,
			BigDecimal balanceAfter,
			Instant createdAt) {
		return new LedgerEntry(id, accountId, transactionRef, Direction.DEBIT, amount, balanceAfter, createdAt);
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

	public UUID getAccountId() {
		return accountId;
	}

	public String getTransactionRef() {
		return transactionRef;
	}

	public EntryType getEntryType() {
		return entryType;
	}

	public Direction getDirection() {
		return direction;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
