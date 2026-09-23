package ru.shim.closedclub.dto.entry;

import java.util.UUID;

public record EntryResponse(
        Long memberId, String fullName, UUID newQrCode
) {
}
