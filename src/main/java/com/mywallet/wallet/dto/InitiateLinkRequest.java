package com.mywallet.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiateLinkRequest(
		@NotBlank @Pattern(regexp = "\\d{12}") String accountNumber) {
}
