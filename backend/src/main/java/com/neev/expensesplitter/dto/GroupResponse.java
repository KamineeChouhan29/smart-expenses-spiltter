package com.neev.expensesplitter.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GroupResponse(
        Long id,
        String name,
        String createdBy,
        LocalDateTime createdAt
) {}
