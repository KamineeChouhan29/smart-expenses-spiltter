package com.neev.expensesplitter.repository;

import com.neev.expensesplitter.model.Split;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SplitRepository extends JpaRepository<Split, Long> {

    List<Split> findByExpenseId(Long expenseId);

    @Query("SELECT s FROM Split s WHERE s.expense.group.id = :groupId")
    List<Split> findByGroupId(@Param("groupId") Long groupId);
}
