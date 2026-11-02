package com.mywallet.wallet.dto;

import java.util.UUID;

public record InitiateLinkResponse(UUID linkId, int expiresIn) {
}
