package ru.shim.closedclub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.shim.closedclub.dto.member.MemberRequest;
import ru.shim.closedclub.dto.member.MemberResponse;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.exception.ResourceNotFoundException;
import ru.shim.closedclub.repository.MemberRepository;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class MemberServiceTest {

    @Test
    void delete_whenMemberExists_setsDeletedAt() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        Member member = new Member();
        long id = 1L;

        when(memberRepository.getByIdOrThrow(id))
                .thenReturn(member);

        memberService.delete(id);
        assertNotNull(member.getDeletedAt());
    }

    @Test
    void delete_whenMemberNotFound_throwsResourceNotFoundException() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        long id = 99L;
        when(memberRepository.getByIdOrThrow(id))
                .thenThrow(new ResourceNotFoundException("Участник с id 99 не найден"));
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> memberService.delete(id));
        assertEquals("Участник с id 99 не найден", exception.getMessage());
    }

    @Test
    void update_whenMemberExists_updatesNamesAndSavesMember() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        long id = 1L;
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        MemberRequest request = new MemberRequest("Пётр", "Иванович", "Иванов");

        when(memberRepository.getByIdOrThrow(id))
                .thenReturn(member);
        when(memberRepository.save(member))
                .thenReturn(member);

        MemberResponse response = memberService.update(id, request);

        assertEquals("Пётр", member.getFirstName());
        assertEquals("Пётр", response.firstName());
        assertEquals("Иванович", member.getMiddleName());
        assertEquals("Иванович", response.middleName());
        assertEquals("Иванов", member.getSurname());
        assertEquals("Иванов", response.surname());

        verify(memberRepository).save(member);
    }

    @Test
    void update_whenMemberNotFound_throwsExceptionAndDoesNotSave() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        long id = 99L;
        MemberRequest request = new MemberRequest("Олег", "Олегович", "Сидоров");
        when(memberRepository.getByIdOrThrow(id))
                .thenThrow(new ResourceNotFoundException("Участник с id 99 не найден"));
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> memberService.update(id, request));
        assertEquals("Участник с id 99 не найден", exception.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    void create_savesMemberWithRequestNamesAndReturnsResponse() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        MemberRequest request = new MemberRequest("Игорь", "Максимович", "Старков");
        Member savedMember = new Member();
        savedMember.setFirstName("Павел");
        savedMember.setMiddleName("Максимович");
        savedMember.setSurname("Сталин");

        when(memberRepository.save(any(Member.class)))
                .thenReturn(savedMember);

        MemberResponse response = memberService.create(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);

        verify(memberRepository).save(captor.capture());

        Member capturedMember = captor.getValue();

        assertEquals(request.firstName(), capturedMember.getFirstName());
        assertEquals(request.middleName(), capturedMember.getMiddleName());
        assertEquals(request.surname(), capturedMember.getSurname());

        assertEquals(savedMember.getFirstName(), response.firstName());
        assertEquals(savedMember.getMiddleName(), response.middleName());
        assertEquals(savedMember.getSurname(), response.surname());
    }

    @Test
    void getById_whenMemberExists_returnsMemberResponse() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        long id = 7L;
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(id);
        when(member.getFirstName()).thenReturn("Анна");
        when(member.getMiddleName()).thenReturn("Андреевна");
        when(member.getSurname()).thenReturn("Орлова");

        when(memberRepository.getByIdOrThrow(id)).thenReturn(member);
        MemberResponse response = memberService.getById(id);

        assertEquals(member.getId(), response.id());
        assertEquals(member.getFirstName(), response.firstName());
        assertEquals(member.getMiddleName(), response.middleName());
        assertEquals(member.getSurname(), response.surname());

        verify(memberRepository).getByIdOrThrow(id);
    }

    @Test
    void getById_whenMemberNotFound_throwsResourceNotFoundException() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        long id = 99L;

        when(memberRepository.getByIdOrThrow(id))
                .thenThrow(new ResourceNotFoundException("Участник с id 99 не найден"));

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> memberService.getById(id));
        assertEquals("Участник с id 99 не найден", exception.getMessage());
        verify(memberRepository).getByIdOrThrow(id);
    }

    @Test
    void getMembers_whenSurnameIsNull_returnsMemberPage() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(7L);
        when(member.getFirstName()).thenReturn("Марк");
        when(member.getMiddleName()).thenReturn("Павлович");
        when(member.getSurname()).thenReturn("Орлов");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> members = new PageImpl<>(List.of(member), pageable, 1L);

        when(memberRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(members);

        Page<MemberResponse> result = memberService.getMembers(null, pageable);

        MemberResponse response = result.getContent().get(0);
        assertEquals(member.getId(), response.id());
        assertEquals(member.getFirstName(), response.firstName());
        assertEquals(member.getMiddleName(), response.middleName());
        assertEquals(member.getSurname(), response.surname());

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());

        verify(memberRepository).findAllByDeletedAtIsNull(pageable);
    }

    @Test
    void getMembers_whenSurnameProvided_usesSurnameFilter() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(7L);
        when(member.getFirstName()).thenReturn("Марк");
        when(member.getMiddleName()).thenReturn("Павлович");
        when(member.getSurname()).thenReturn("Орлов");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> members = new PageImpl<>(List.of(member), pageable, 1L);

        String surname = "орл";

        when(memberRepository.findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(surname, pageable))
                .thenReturn(members);

        Page<MemberResponse> result = memberService.getMembers(surname, pageable);

        MemberResponse response = result.getContent().get(0);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(member.getId(), response.id());
        assertEquals(member.getFirstName(), response.firstName());
        assertEquals(member.getMiddleName(), response.middleName());
        assertEquals(member.getSurname(), response.surname());

        verify(memberRepository).findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(surname, pageable);
        verify(memberRepository, never()).findAllByDeletedAtIsNull(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void getMembers_whenSurnameIsBlank_usesUnfilteredQuery(String surname) {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(7L);
        when(member.getFirstName()).thenReturn("Марк");
        when(member.getMiddleName()).thenReturn("Павлович");
        when(member.getSurname()).thenReturn("Орлов");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> members = new PageImpl<>(List.of(member), pageable, 1L);

        when(memberRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(members);

        Page<MemberResponse> result = memberService.getMembers(surname, pageable);

        MemberResponse response = result.getContent().get(0);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());

        assertEquals(member.getId(), response.id());
        assertEquals(member.getFirstName(), response.firstName());
        assertEquals(member.getMiddleName(), response.middleName());
        assertEquals(member.getSurname(), response.surname());

        verify(memberRepository).findAllByDeletedAtIsNull(pageable);
        verify(memberRepository, never()).findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(any(), any());
    }

    @Test
    void getMembers_whenNoMembersMatch_returnsEmptyPage() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberService memberService = new MemberService(memberRepository);

        Pageable pageable = PageRequest.of(0, 10);
        String surname = "Несуществующая";
        Page<Member> members = new PageImpl<>(List.of(), pageable, 0L);

        when(memberRepository.findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(surname, pageable)).thenReturn(members);

        Page<MemberResponse> result = memberService.getMembers(surname, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        verify(memberRepository).findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(surname, pageable);
    }
}
