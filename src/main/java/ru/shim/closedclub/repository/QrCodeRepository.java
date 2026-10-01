package ru.shim.closedclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.QrCode;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByCode(UUID code);

    Page<QrCode> findByMemberId(Long memberId, Pageable pageable);

}
