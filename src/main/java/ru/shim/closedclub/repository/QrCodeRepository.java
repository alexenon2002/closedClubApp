package ru.shim.closedclub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.QrCode;

import java.util.Optional;
import java.util.UUID;

public interface QrCodeRepository extends JpaRepository <QrCode,Long> {
    Optional<QrCode> findByCode(UUID code);
}
