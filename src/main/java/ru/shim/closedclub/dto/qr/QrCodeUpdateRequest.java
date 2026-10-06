package ru.shim.closedclub.dto.qr;

import jakarta.validation.constraints.NotNull;

public record QrCodeUpdateRequest(@NotNull Long memberId) {
}
