package ru.shim.closedclub.dto.entry;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EntryRequest(@NotNull UUID code){
    }

