package ru.shim.closedclub.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.shim.closedclub.dto.qr.QrCodeCreateRequest;
import ru.shim.closedclub.dto.qr.QrCodeResponse;
import ru.shim.closedclub.dto.qr.QrCodeUpdateRequest;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.exception.ResourceNotFoundException;
import ru.shim.closedclub.repository.MemberRepository;
import ru.shim.closedclub.repository.QrCodeRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class QrCodeServiceTest {

    @Test
    void delete_whenQrCodeExists_deletesQrCode() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        QrCode qrCode = new QrCode();
        long id = 1L;

        when(qrCodeRepository.getByIdOrThrow(id)).thenReturn(qrCode);
        qrCodeService.delete(id);

        verify(qrCodeRepository).getByIdOrThrow(id);
        verify(qrCodeRepository).delete(qrCode);
    }

    @Test
    void delete_whenQrCodeNotFound_throwsExceptionAndDoesNotDelete() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long id = 99L;
        when(qrCodeRepository.getByIdOrThrow(id))
                .thenThrow(new ResourceNotFoundException("QR-код с id 99 не найден"));
        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> qrCodeService.delete(id));

        assertEquals("QR-код с id 99 не найден", exception.getMessage());

        verify(qrCodeRepository).getByIdOrThrow(id);
        verify(qrCodeRepository, never()).delete(any());
    }

    @Test
    void getById_whenQrCodeExists_returnsQrCodeResponse() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long qrCodeId = 8L;
        long memberId = 1L;
        UUID code = UUID.randomUUID();
        Member member = mock(Member.class);
        QrCode qrCode = mock(QrCode.class);
        when(member.getId()).thenReturn(memberId);
        when(qrCode.getId()).thenReturn(qrCodeId);
        when(qrCode.getCode()).thenReturn(code);
        when(qrCode.getMember()).thenReturn(member);

        when(qrCodeRepository.getByIdOrThrow(qrCodeId)).thenReturn(qrCode);

        QrCodeResponse response = qrCodeService.getById(qrCodeId);

        assertEquals(qrCodeId, response.id());
        assertEquals(memberId, response.memberId());
        assertEquals(code, response.code());

        verify(qrCodeRepository).getByIdOrThrow(qrCodeId);
    }

    @Test
    void getById_whenQrCodeNotFound_throwsResourceNotFoundException() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long id = 99L;

        when(qrCodeRepository.getByIdOrThrow(id))
                .thenThrow(new ResourceNotFoundException("QR-код с id 99 не найден"));
        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> qrCodeService.getById(id));

        assertEquals("QR-код с id 99 не найден", exception.getMessage());

        verify(qrCodeRepository).getByIdOrThrow(id);
    }

    @Test
    void create_whenMemberExists_generatesCodeAndSavesQrCode() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        Member member = mock(Member.class);
        long memberId = 1L;
        when(member.getId()).thenReturn(memberId);

        QrCodeCreateRequest request = new QrCodeCreateRequest(memberId);

        when(memberRepository.getByIdOrThrow(memberId))
                .thenReturn(member);

        when(qrCodeRepository.save(any(QrCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QrCodeResponse response = qrCodeService.create(request);

        ArgumentCaptor<QrCode> captor = ArgumentCaptor.forClass(QrCode.class);

        verify(qrCodeRepository).save(captor.capture());

        QrCode capturedQrCode = captor.getValue();

        assertNotNull(capturedQrCode.getCode());
        assertSame(member, capturedQrCode.getMember());
        assertEquals(capturedQrCode.getCode(), response.code());
        assertEquals(memberId, response.memberId());

        verify(memberRepository).getByIdOrThrow(memberId);
    }

    @Test
    void create_whenMemberNotFound_throwsExceptionAndDoesNotSave() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long memberId = 99L;

        QrCodeCreateRequest request = new QrCodeCreateRequest(memberId);

        when(memberRepository.getByIdOrThrow(memberId))
                .thenThrow(new ResourceNotFoundException("Участник с id 99 не найден"));
        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> qrCodeService.create(request));

        assertEquals("Участник с id 99 не найден", exception.getMessage());

        verify(memberRepository).getByIdOrThrow(memberId);
        verify(qrCodeRepository, never()).save(any(QrCode.class));

    }

    @Test
    void update_whenQrCodeAndMemberExist_changesMemberAndKeepsCode() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long qrCodeId = 8L;
        long newMemberId = 1L;
        UUID originalCode = UUID.randomUUID();

        Member oldMember = new Member();
        Member newMember = mock(Member.class);

        when(newMember.getId()).thenReturn(newMemberId);

        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(oldMember);

        QrCodeUpdateRequest request = new QrCodeUpdateRequest(newMemberId);
        when(memberRepository.getByIdOrThrow(newMemberId)).thenReturn(newMember);
        when(qrCodeRepository.getByIdOrThrow(qrCodeId)).thenReturn(qrCode);
        when(qrCodeRepository.save(qrCode)).thenReturn(qrCode);

        QrCodeResponse response = qrCodeService.update(qrCodeId, request);

        assertSame(newMember, qrCode.getMember());
        assertEquals(originalCode, qrCode.getCode());
        assertEquals(originalCode, response.code());
        assertEquals(newMemberId, response.memberId());

        verify(memberRepository).getByIdOrThrow(newMemberId);
        verify(qrCodeRepository).getByIdOrThrow(qrCodeId);
        verify(qrCodeRepository).save(qrCode);
    }

    @Test
    void update_whenQrCodeNotFound_throwsExceptionAndDoesNotSave() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);

        long qrCodeId = 99L;
        long memberId = 1L;
        QrCodeUpdateRequest request = new QrCodeUpdateRequest(memberId);

        when(qrCodeRepository.getByIdOrThrow(qrCodeId))
                .thenThrow(new ResourceNotFoundException("QR-код с id 99 не найден"));

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> qrCodeService.update(qrCodeId, request));

        assertEquals("QR-код с id 99 не найден", exception.getMessage());

        verify(qrCodeRepository).getByIdOrThrow(qrCodeId);
        verify(memberRepository, never()).getByIdOrThrow(any());
        verify(qrCodeRepository, never()).save(any());
    }

    @Test
    void update_whenMemberNotFound_throwsExceptionAndKeepsQrCodeUnchanged() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);


        long qrCodeId = 11L;
        long newMemberId = 55L;
        UUID originalCode = UUID.randomUUID();

        Member oldMember = new Member();

        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(oldMember);

        QrCodeUpdateRequest request = new QrCodeUpdateRequest(newMemberId);

        when(qrCodeRepository.getByIdOrThrow(qrCodeId)).thenReturn(qrCode);
        when(memberRepository.getByIdOrThrow(newMemberId))
                .thenThrow(new ResourceNotFoundException("Участник с id 55 не найден"));

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> qrCodeService.update(qrCodeId, request));

        assertEquals("Участник с id 55 не найден", exception.getMessage());

        assertSame(oldMember, qrCode.getMember());
        assertEquals(originalCode, qrCode.getCode());

        verify(qrCodeRepository).getByIdOrThrow(qrCodeId);
        verify(memberRepository).getByIdOrThrow(newMemberId);
        verify(qrCodeRepository, never()).save(any());
    }

    @Test
    void getQrCodes_whenMemberIdIsNull_returnsQrCodePage() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long qrCodeId = 9L;
        long memberId = 8L;
        UUID code = UUID.randomUUID();

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);

        QrCode qrCode = mock(QrCode.class);
        when(qrCode.getCode()).thenReturn(code);
        when(qrCode.getMember()).thenReturn(member);
        when(qrCode.getId()).thenReturn(qrCodeId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<QrCode> qrCodes = new PageImpl<>(List.of(qrCode), pageable, 1L);

        when(qrCodeRepository.findAll(pageable)).thenReturn(qrCodes);

        Page<QrCodeResponse> result = qrCodeService.getQrCodes(null, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        QrCodeResponse response = result.getContent().get(0);

        assertEquals(qrCodeId, response.id());
        assertEquals(code, response.code());
        assertEquals(memberId, response.memberId());

        verify(qrCodeRepository).findAll(pageable);
        verify(qrCodeRepository, never()).findByMemberId(any(), any());
    }

    @Test
    void getQrCodes_whenMemberIdProvided_usesMemberFilter() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);
        long qrCodeId = 9L;
        long memberId = 8L;
        UUID code = UUID.randomUUID();

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);

        QrCode qrCode = mock(QrCode.class);
        when(qrCode.getCode()).thenReturn(code);
        when(qrCode.getMember()).thenReturn(member);
        when(qrCode.getId()).thenReturn(qrCodeId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<QrCode> qrCodes = new PageImpl<>(List.of(qrCode), pageable, 1L);

        when(qrCodeRepository.findByMemberId(memberId, pageable))
                .thenReturn(qrCodes);

        Page<QrCodeResponse> result = qrCodeService.getQrCodes(memberId, pageable);

        assertEquals(1L, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        QrCodeResponse response = result.getContent().get(0);

        assertEquals(1, result.getContent().size());
        assertEquals(qrCodeId, response.id());
        assertEquals(code, response.code());
        assertEquals(memberId, response.memberId());

        verify(qrCodeRepository).findByMemberId(memberId, pageable);
        verify(qrCodeRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getQrCodes_whenNoQrCodesMatch_returnsEmptyPage() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        QrCodeRepository qrCodeRepository = mock(QrCodeRepository.class);
        QrCodeService qrCodeService = new QrCodeService(memberRepository, qrCodeRepository);

        long memberId = 99L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<QrCode> qrCodes = new PageImpl<>(List.of(), pageable, 0L);

        when(qrCodeRepository.findByMemberId(memberId, pageable))
                .thenReturn(qrCodes);

        Page<QrCodeResponse> result = qrCodeService.getQrCodes(memberId, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        verify(qrCodeRepository).findByMemberId(memberId, pageable);
        verify(qrCodeRepository, never()).findAll(any(Pageable.class));
    }
}
