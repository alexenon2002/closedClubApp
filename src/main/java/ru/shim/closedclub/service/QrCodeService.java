package ru.shim.closedclub.service;

import org.springframework.stereotype.Service;
import ru.shim.closedclub.repository.MemberRepository;

@Service
public class QrCodeService {
    private final MemberRepository memberRepository;

    public QrCodeService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
