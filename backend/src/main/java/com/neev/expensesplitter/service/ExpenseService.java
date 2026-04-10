package com.neev.expensesplitter.service;

import com.neev.expensesplitter.dto.ExpenseRequest;
import com.neev.expensesplitter.model.*;
import com.neev.expensesplitter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AIService aiService;

    // ── Add expense ───────────────────────────────────────────────────────────

    @Transactional
    public Expense addExpense(ExpenseRequest req, User requestingUser) {
        ExpenseGroup group = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(req.getGroupId(),
                requestingUser.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        User paidBy = userRepository.findById(req.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        // ── AI categorization (non-blocking on failure) ───────────────────────
        String category = aiService.categorizeExpense(req.getDescription());

        Expense expense = Expense.builder()
                .group(group)
                .description(req.getDescription())
                .amount(req.getAmount())
                .paidBy(paidBy)
                .category(category)
                .splitType(Expense.SplitType.valueOf(req.getSplitType()))
                .build();

        expense = expenseRepository.save(expense);

        // ── Create splits ─────────────────────────────────────────────────────
        if ("EQUAL".equals(req.getSplitType())) {
            createEqualSplits(expense, req.getGroupId());
        } else {
            createCustomSplits(expense, req);
        }

        return expense;
    }

    @Transactional
    public Expense updateExpense(Long expenseId, ExpenseRequest req, User requestingUser) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(expense.getGroup().getId(), requestingUser.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        User paidBy = userRepository.findById(req.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        if (!expense.getDescription().trim().equalsIgnoreCase(req.getDescription().trim())) {
             expense.setCategory(aiService.categorizeExpense(req.getDescription()));
        }

        expense.setDescription(req.getDescription());
        expense.setAmount(req.getAmount());
        expense.setPaidBy(paidBy);
        expense.setSplitType(Expense.SplitType.valueOf(req.getSplitType()));

        expense = expenseRepository.save(expense);

        List<Split> oldSplits = splitRepository.findByExpenseId(expenseId);
        splitRepository.deleteAll(oldSplits);

        if ("EQUAL".equals(req.getSplitType())) {
            createEqualSplits(expense, expense.getGroup().getId());
        } else {
            createCustomSplits(expense, req);
        }

        return expense;
    }

    @Transactional
    public void deleteExpense(Long expenseId, User requestingUser) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!groupMemberRepository.existsByGroupIdAndUserId(expense.getGroup().getId(), requestingUser.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }
        List<Split> splits = splitRepository.findByExpenseId(expenseId);
        splitRepository.deleteAll(splits);
        expenseRepository.deleteById(expenseId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Expense> getGroupExpenses(Long groupId, User requestingUser) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, requestingUser.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    // ── AI Insights ───────────────────────────────────────────────────────────

    public List<String> getInsights(User user) {
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime lastStart = now.minusDays(14);

        List<Expense> thisWeek = expenseRepository
                .findExpensesByUserAndDateRange(user.getId(), weekStart, now);
        List<Expense> lastWeek = expenseRepository
                .findExpensesByUserAndDateRange(user.getId(), lastStart, weekStart);

        Map<String, BigDecimal> thisMap = groupByCategory(thisWeek);
        Map<String, BigDecimal> lastMap = groupByCategory(lastWeek);

        return aiService.generateInsights(thisMap, lastMap);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void createEqualSplits(Expense expense, Long groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        int count = members.size();
        BigDecimal each = expense.getAmount()
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        members.forEach(member ->
                splitRepository.save(Split.builder()
                        .expense(expense)
                        .user(member.getUser())
                        .amountOwed(each)
                        .build()));
    }

    private void createCustomSplits(Expense expense, ExpenseRequest req) {
        boolean isPercentage = "PERCENTAGE".equalsIgnoreCase(req.getSplitMode());

        req.getSplits().forEach(entry -> {
            User user = userRepository.findById(entry.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + entry.getUserId()));

            BigDecimal owed;
            if (isPercentage) {
                owed = expense.getAmount()
                        .multiply(entry.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                owed = entry.getAmount();
            }

            splitRepository.save(Split.builder()
                    .expense(expense)
                    .user(user)
                    .amountOwed(owed)
                    .build());
        });
    }

    private Map<String, BigDecimal> groupByCategory(List<Expense> expenses) {
        return expenses.stream().collect(Collectors.groupingBy(
                Expense::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
        ));
    }
}
