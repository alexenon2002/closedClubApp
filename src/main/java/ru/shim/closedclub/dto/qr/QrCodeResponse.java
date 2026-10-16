package ru.shim.closedclub.dto.qr;

import java.util.UUID;

public record QrCodeResponse(Long id, UUID code, Long memberId) {
}
