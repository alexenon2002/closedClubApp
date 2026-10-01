package ru.shim.closedclub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shim.closedclub.dto.entry.EntryResponse;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.exception.AccessDeniedException;
import ru.shim.closedclub.repository.QrCodeRepository;

import java.util.UUID;

@Service
public class EntryService {
    private final QrCodeRepository qrCodeRepository;

    public EntryService(final QrCodeRepository qrCodeRepository) {
        this.qrCodeRepository = qrCodeRepository;
    }

    @Transactional
    public EntryResponse enter(UUID code) {
        QrCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new AccessDeniedException("Вход запрещён: QR-код не найден"));
        Member member = qrCode.getMember();
        UUID newCode = UUID.randomUUID();
        qrCode.setCode(newCode);
        return new EntryResponse(member.getId(), member.getFullName(), newCode);
    }
}
