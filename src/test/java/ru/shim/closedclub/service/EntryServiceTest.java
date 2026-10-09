package ru.shim.closedclub.service;

import org.junit.jupiter.api.Test;
import ru.shim.closedclub.dto.entry.EntryResponse;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.exception.AccessDeniedException;
import ru.shim.closedclub.repository.QrCodeRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntryServiceTest {

    @Test
    void enter_wheQrCodeNotFound_throwsAccessDeniedException() {
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        EntryService entryService = new EntryService(qrCodeRepository);
        UUID uuid = UUID.randomUUID();

        when(qrCodeRepository.findByCode(uuid))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> entryService.enter(uuid));
    }

    @Test
    void enter_whenMemberDeleted_throwsAccessDeniedException() {
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        EntryService entryService = new EntryService(qrCodeRepository);
        UUID uuid = UUID.randomUUID();
        Member member = new Member();
        member.setDeletedAt(LocalDateTime.now());
        QrCode qrCode = new QrCode();
        qrCode.setMember(member);
        qrCode.setCode(uuid);

        when(qrCodeRepository.findByCode(uuid))
                .thenReturn(Optional.of(qrCode));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> entryService.enter(uuid));

        assertEquals("Вход запрещён: участник удалён", exception.getMessage());
        assertEquals(uuid, qrCode.getCode());
    }

    @Test
    void enter_whenMemberActive_returnsFullNameAndRotatesCode() {
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        EntryService entryService = new EntryService(qrCodeRepository);
        UUID uuid = UUID.randomUUID();
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        QrCode qrCode = new QrCode();
        qrCode.setMember(member);
        qrCode.setCode(uuid);

        when(qrCodeRepository.findByCode(uuid))
                .thenReturn(Optional.of(qrCode));

        EntryResponse response = entryService.enter(uuid);

        assertEquals("Иван Петрович Петров", response.fullName());
        assertNotNull(response.newQrCode());
        assertNotEquals(uuid, response.newQrCode());
        assertEquals(qrCode.getCode(), response.newQrCode());
    }
}
