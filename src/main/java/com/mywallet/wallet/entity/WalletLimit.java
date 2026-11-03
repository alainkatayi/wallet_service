package com.mywallet.wallet.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet_limit")
public class WalletLimit {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false, length = 10)
	private OperationType operationType;

	@Column(name = "min_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal maxAmount;

	@Column(name = "daily_max", nullable = false, precision = 19, scale = 2)
	private BigDecimal dailyMax;

	protected WalletLimit() {
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public BigDecimal getMinAmount() {
		return minAmount;
	}

	public BigDecimal getMaxAmount() {
		return maxAmount;
	}

	public BigDecimal getDailyMax() {
		return dailyMax;
	}

}
