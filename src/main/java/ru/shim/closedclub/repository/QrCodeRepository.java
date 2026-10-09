package ru.shim.closedclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.exception.ResourceNotFoundException;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByCode(UUID code);

    Page<QrCode> findByMemberId(Long memberId, Pageable pageable);

    default QrCode getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(()-> new ResourceNotFoundException("QR-код с id " + id + " не найден"));
    }

}
