package com.neev.expensesplitter.dto;

public record JwtResponse(
        String token,
        Long id,
        String username,
        String email
) {}
