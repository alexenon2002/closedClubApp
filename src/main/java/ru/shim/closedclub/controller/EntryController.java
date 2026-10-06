package ru.shim.closedclub.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.shim.closedclub.dto.entry.EntryRequest;
import ru.shim.closedclub.dto.entry.EntryResponse;
import ru.shim.closedclub.service.EntryService;

@RestController
@RequestMapping("/api/v1/entry")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @PostMapping
    public EntryResponse enter(@Valid @RequestBody EntryRequest entryRequest) {
        return entryService.enter(entryRequest.code());
    }
}
