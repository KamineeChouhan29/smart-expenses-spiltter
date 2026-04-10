package com.neev.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
        @NotBlank String username
) {}
