package com.neev.expensesplitter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExpenseRequest {

    @NotNull
    private Long groupId;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;        // always INR (₹)

    @NotNull
    private Long paidByUserId;

    @NotBlank
    private String splitType;         // "EQUAL" | "CUSTOM"

    private String splitMode;         // "AMOUNT" | "PERCENTAGE"  (only for CUSTOM)

    private List<SplitEntry> splits;  // only for CUSTOM

    @Data
    public static class SplitEntry {
        @NotNull
        private Long userId;
        private BigDecimal amount;      // set when splitMode = AMOUNT
        private BigDecimal percentage;  // set when splitMode = PERCENTAGE
    }
}
