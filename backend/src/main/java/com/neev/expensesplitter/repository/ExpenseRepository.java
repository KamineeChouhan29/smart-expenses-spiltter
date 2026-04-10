package com.neev.expensesplitter.repository;

import com.neev.expensesplitter.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    /**
     * Fetch expenses within a date range where the current user is a member
     * of that expense's group (used for AI insights).
     */
    @Query("""
        SELECT DISTINCT e FROM Expense e
        JOIN GroupMember gm ON e.group.id = gm.group.id
        WHERE gm.user.id = :userId
          AND e.createdAt BETWEEN :start AND :end
        ORDER BY e.createdAt DESC
        """)
    List<Expense> findExpensesByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
