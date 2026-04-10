package com.neev.expensesplitter.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,        // INR
        String paidBy,
        String category,
        String splitType,
        LocalDateTime createdAt,
        List<SplitResponse> splits
) {}
