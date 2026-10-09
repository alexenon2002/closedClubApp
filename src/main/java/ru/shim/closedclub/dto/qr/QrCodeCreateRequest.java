package ru.shim.closedclub.dto.qr;

import jakarta.validation.constraints.NotNull;

public record QrCodeCreateRequest(@NotNull Long memberId) {
}
