package com.neev.expensesplitter.dto;

import java.math.BigDecimal;

public record SplitResponse(Long userId, String username, BigDecimal amountOwed) {}
