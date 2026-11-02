package com.mywallet.wallet.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmLinkRequest(
		@NotNull UUID linkId,
		@NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
