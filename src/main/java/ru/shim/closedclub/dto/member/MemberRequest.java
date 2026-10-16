package ru.shim.closedclub.dto.member;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest(@NotBlank String firstName, String middleName, @NotBlank String surname) {
}

