package ru.shim.closedclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.shim.closedclub.dto.member.MemberRequest;
import ru.shim.closedclub.dto.member.MemberResponse;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.exception.ResourceNotFoundException;
import ru.shim.closedclub.repository.MemberRepository;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberResponse getById(long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Участник с id " + id + " не найден"));
        return new MemberResponse(
                member.getId(),
                member.getFullName()
        );
    }

    public Page<MemberResponse> getMembers(String fullName, Pageable pageable) {
        Page<Member> members;
        if (fullName == null || fullName.isBlank()) {
            members = memberRepository
                    .findAll(pageable);
        } else {
            members = memberRepository
                    .findByFullNameContainingIgnoreCase(fullName, pageable);
        }
        return members.map(member ->
                new MemberResponse(
                        member.getId(),
                        member.getFullName()
                )
        );
    }

    public MemberResponse create(MemberRequest memberRequest) {
        Member member = new Member();

        member.setFullName(memberRequest.fullName());

        Member savedMember = memberRepository.save(member);

        return new MemberResponse(
                savedMember.getId(),
                savedMember.getFullName()
        );
    }

    public MemberResponse update(long id, MemberRequest memberRequest) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Участник с id " + id + " не найден"));
        member.setFullName(memberRequest.fullName());
        Member updateMember = memberRepository.save(member);
        return new MemberResponse(updateMember.getId(), updateMember.getFullName());
    }

    public void delete(long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Участник с id " + id + " не найден"));
        memberRepository.delete(member);
    }
}
