package com.felipeaugdev;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByActiveTrueAndNextOccurrenceDateLessThanEqual(LocalDate date);

    List<RecurringExpense> findByUserAndActiveTrue(User user);

    List<RecurringExpense> findByUserIsNullAndActiveTrue();
}
