package com.mywallet.wallet.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MovementResponse(
		String status,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balanceAfter,
		String reason) {

	public static MovementResponse applied(BigDecimal balanceAfter) {
		return new MovementResponse("APPLIED", balanceAfter, null);
	}

	public static MovementResponse rejected(String reason) {
		return new MovementResponse("REJECTED", null, reason);
	}

}
