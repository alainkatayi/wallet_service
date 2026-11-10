package com.mywallet.wallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.mywallet.wallet.dto.MovementResponse;
import com.mywallet.wallet.exception.InvalidRequestException;

import tools.jackson.databind.JsonNode;

@Service
public class WalletTransferService {

	private final WalletTransferWriter writer;

	public WalletTransferService(WalletTransferWriter writer) {
		this.writer = writer;
	}

	public MovementResponse debit(UUID userId, JsonNode body) {
		String transactionRef = text(body, "transactionRef");
		BigDecimal amount = amount(body);
		if (transactionRef.length() < 1 || transactionRef.length() > 64) {
			throw new InvalidRequestException();
		}
		try {
			return writer.debit(userId, transactionRef, amount);
		}
		catch (DataIntegrityViolationException exception) {
			if (!WalletCreditWriter.ledgerConflict(exception)) {
				throw exception;
			}
			return writer.replayDebit(userId, transactionRef, amount);
		}
	}

	public MovementResponse credit(UUID userId, JsonNode body) {
		String transactionRef = text(body, "transactionRef");
		BigDecimal amount = amount(body);
		if (transactionRef.length() < 1 || transactionRef.length() > 64) {
			throw new InvalidRequestException();
		}
		try {
			return writer.credit(userId, transactionRef, amount);
		}
		catch (DataIntegrityViolationException exception) {
			if (!WalletCreditWriter.ledgerConflict(exception)) {
				throw exception;
			}
			return writer.replayCredit(userId, transactionRef, amount);
		}
	}

	public MovementResponse reverse(UUID userId, JsonNode body) {
		String transactionRef = text(body, "transactionRef");
		if (transactionRef.length() < 1 || transactionRef.length() > 64) {
			throw new InvalidRequestException();
		}
		try {
			return writer.reverse(userId, transactionRef);
		}
		catch (DataIntegrityViolationException exception) {
			if (!WalletCreditWriter.ledgerConflict(exception)) {
				throw exception;
			}
			return writer.replayReverse(userId, transactionRef);
		}
	}

	private static String text(JsonNode body, String field) {
		if (body == null || !body.isObject()) {
			throw new InvalidRequestException();
		}
		JsonNode node = body.get(field);
		if (node == null || !node.isString()) {
			throw new InvalidRequestException();
		}
		String value = node.asString().trim();
		if (value.isEmpty() || value.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F)) {
			throw new InvalidRequestException();
		}
		return value;
	}

	private static BigDecimal amount(JsonNode body) {
		String text = text(body, "amount");
		if (!text.matches("(?:0|[1-9]\\d*)(?:\\.\\d{1,2})?")) {
			throw new InvalidRequestException();
		}
		try {
			BigDecimal value = new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
			if (value.signum() <= 0) {
				throw new InvalidRequestException();
			}
			return value;
		}
		catch (NumberFormatException | ArithmeticException exception) {
			throw new InvalidRequestException();
		}
	}

}
