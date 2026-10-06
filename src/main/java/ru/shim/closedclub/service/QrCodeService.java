package ru.shim.closedclub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.shim.closedclub.dto.qr.QrCodeCreateRequest;
import ru.shim.closedclub.dto.qr.QrCodeResponse;
import ru.shim.closedclub.dto.qr.QrCodeUpdateRequest;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.repository.MemberRepository;
import ru.shim.closedclub.repository.QrCodeRepository;

import java.util.UUID;

@Service
public class QrCodeService {
    private final MemberRepository memberRepository;
    private final QrCodeRepository qrCodeRepository;

    public QrCodeService(MemberRepository memberRepository, QrCodeRepository qrCodeRepository) {
        this.memberRepository = memberRepository;
        this.qrCodeRepository = qrCodeRepository;
    }

    public Page<QrCodeResponse> getQrCodes(Long memberId, Pageable pageable) {
        Page<QrCode> qrCodes;
        if (memberId == null) {
            qrCodes = qrCodeRepository.findAll(pageable);
        } else {
            qrCodes = qrCodeRepository.findByMemberId(memberId, pageable);
        }
        return qrCodes.map(qrCode
                -> new QrCodeResponse(qrCode.getId(), qrCode.getCode(), qrCode.getMember().getId()));
    }

    public QrCodeResponse getById(long id) {
        QrCode qrCode = qrCodeRepository.getByIdOrThrow(id);
        return new QrCodeResponse(
                qrCode.getId(),
                qrCode.getCode(),
                qrCode.getMember().getId()
        );
    }

    public QrCodeResponse create(QrCodeCreateRequest qrCodeCreateRequest) {
        Member member = memberRepository.getByIdOrThrow(qrCodeCreateRequest.memberId());
        QrCode qrCode = new QrCode();
        qrCode.setCode(UUID.randomUUID());
        qrCode.setMember(member);
        QrCode savedQrCode = qrCodeRepository.save(qrCode);
        return new QrCodeResponse(savedQrCode.getId(), savedQrCode.getCode(), savedQrCode.getMember().getId());
    }

    public QrCodeResponse update(Long id, QrCodeUpdateRequest request) {
        QrCode qrCode = qrCodeRepository.getByIdOrThrow(id);
        Member member = memberRepository.getByIdOrThrow(request.memberId());
        qrCode.setMember(member);
        QrCode updatedQrCode = qrCodeRepository.save(qrCode);
        return new QrCodeResponse(updatedQrCode.getId(), updatedQrCode.getCode(), updatedQrCode.getMember().getId());
    }

    public void delete(Long id) {
        QrCode qrCode = qrCodeRepository.getByIdOrThrow(id);
        qrCodeRepository.delete(qrCode);
    }
}
