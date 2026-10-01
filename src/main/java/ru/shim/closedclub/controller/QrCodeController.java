package ru.shim.closedclub.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.shim.closedclub.dto.qr.QrCodeCreateRequest;
import ru.shim.closedclub.dto.qr.QrCodeResponse;
import ru.shim.closedclub.dto.qr.QrCodeUpdateRequest;
import ru.shim.closedclub.service.QrCodeService;

@RestController
@RequestMapping("/api/qr-codes")

public class QrCodeController {
    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;

    }

    @PostMapping
    public QrCodeResponse create(@RequestBody QrCodeCreateRequest request) {
        return qrCodeService.create(request);
    }

    @GetMapping
    public Page<QrCodeResponse> getQrCodes(@RequestParam(required = false) Long memberId, Pageable pageable) {
        return qrCodeService.getQrCodes(memberId, pageable);
    }

    @GetMapping("/{id}")
    public QrCodeResponse getById(@PathVariable long id) {
        return qrCodeService.getById(id);
    }

    @PutMapping("/{id}")
    public QrCodeResponse update(@PathVariable long id, @RequestBody QrCodeUpdateRequest request) {
        return qrCodeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        qrCodeService.delete(id);
    }
}
