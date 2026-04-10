package com.neev.expensesplitter.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String debtor,    // username of person who owes
        String creditor,  // username of person to be paid
        BigDecimal amount // amount in INR
) {}
