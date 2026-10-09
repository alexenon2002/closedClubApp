package ru.shim.closedclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shim.closedclub.dto.member.MemberRequest;
import ru.shim.closedclub.dto.member.MemberResponse;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.repository.MemberRepository;

import java.time.LocalDateTime;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberResponse getById(long id) {
        Member member = memberRepository.getByIdOrThrow(id);
        return new MemberResponse(
                member.getId(),
                member.getFirstName(),
                member.getMiddleName(),
                member.getSurname()
        );
    }

    public Page<MemberResponse> getMembers(String surname, Pageable pageable) {
        Page<Member> members;
        if (surname == null || surname.isBlank()) {
            members = memberRepository
                    .findAllByDeletedAtIsNull(pageable);
        } else {
            members = memberRepository
                    .findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(surname, pageable);
        }
        return members.map(member ->
                new MemberResponse(
                        member.getId(),
                        member.getFirstName(),
                        member.getMiddleName(),
                        member.getSurname()
                )
        );
    }

    public MemberResponse create(MemberRequest memberRequest) {
        Member member = new Member();
        member.setFirstName(memberRequest.firstName());
        member.setMiddleName(memberRequest.middleName());
        member.setSurname(memberRequest.surname());

        Member savedMember = memberRepository.save(member);

        return new MemberResponse(
                savedMember.getId(),
                savedMember.getFirstName(), savedMember.getMiddleName(), savedMember.getSurname()
        );
    }

    public MemberResponse update(long id, MemberRequest memberRequest) {
        Member member = memberRepository.getByIdOrThrow(id);
        member.setFirstName(memberRequest.firstName());
        member.setMiddleName(memberRequest.middleName());
        member.setSurname(memberRequest.surname());

        Member updateMember = memberRepository.save(member);
        return new MemberResponse(updateMember.getId(),
                updateMember.getFirstName(), updateMember.getMiddleName(), updateMember.getSurname());
    }

    @Transactional
    public void delete(long id) {
        Member member = memberRepository.getByIdOrThrow(id);
        member.setDeletedAt(LocalDateTime.now());
    }
}
