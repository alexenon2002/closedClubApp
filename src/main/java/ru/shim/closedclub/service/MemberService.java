package ru.shim.closedclub.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.repository.MemberRepository;

import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAll() {
        return memberRepository.findAll();
    }

    public Member getById(@RequestParam long id) {
        return memberRepository.findById(id).orElse(null);
    }
}
