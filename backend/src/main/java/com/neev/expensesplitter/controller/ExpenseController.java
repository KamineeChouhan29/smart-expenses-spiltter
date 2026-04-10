package com.neev.expensesplitter.controller;

import com.neev.expensesplitter.dto.ExpenseRequest;
import com.neev.expensesplitter.dto.ExpenseResponse;
import com.neev.expensesplitter.dto.SplitResponse;
import com.neev.expensesplitter.model.Expense;
import com.neev.expensesplitter.model.Split;
import com.neev.expensesplitter.model.User;
import com.neev.expensesplitter.repository.SplitRepository;
import com.neev.expensesplitter.repository.UserRepository;
import com.neev.expensesplitter.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;
    private final SplitRepository splitRepository;

    // ── POST /api/expenses  →  add expense (triggers AI categorization) ───────
    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody ExpenseRequest request,
                                                      Principal principal) {
        User user = resolveUser(principal);
        Expense expense = expenseService.addExpense(request, user);
        return ResponseEntity.ok(toResponse(expense));
    }

    // ── PUT /api/expenses/{id}  →  update expense ─────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
                                                         @Valid @RequestBody ExpenseRequest request,
                                                         Principal principal) {
        User user = resolveUser(principal);
        Expense expense = expenseService.updateExpense(id, request, user);
        return ResponseEntity.ok(toResponse(expense));
    }

    // ── GET  /api/expenses/group/{groupId}  →  list group expenses ────────────
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(@PathVariable Long groupId,
                                                                  Principal principal) {
        User user = resolveUser(principal);
        List<ExpenseResponse> list = expenseService.getGroupExpenses(groupId, user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ── GET  /api/expenses/insights  →  AI spending insights (current user) ───
    @GetMapping("/insights")
    public ResponseEntity<List<String>> getInsights(Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(expenseService.getInsights(user));
    }

    // ── DELETE /api/expenses/{id}  →  delete an expense ───────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, Principal principal) {
        User user = resolveUser(principal);
        expenseService.deleteExpense(id, user);
        return ResponseEntity.ok().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<SplitResponse> splits = splitRepository.findByExpenseId(expense.getId())
                .stream()
                .map(s -> new SplitResponse(
                        s.getUser().getId(),
                        s.getUser().getUsername(),
                        s.getAmountOwed()))
                .collect(Collectors.toList());

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getUsername(),
                expense.getCategory(),
                expense.getSplitType().name(),
                expense.getCreatedAt(),
                splits);
    }
}
