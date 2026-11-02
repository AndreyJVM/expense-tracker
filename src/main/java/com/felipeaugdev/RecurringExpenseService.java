package com.felipeaugdev;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringRepository;
    private final ExpenseRepository expenseRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringRepository,
            ExpenseRepository expenseRepository) {

        this.recurringRepository = recurringRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Creates a new recurring template.
     * nextOccurrenceDate is calculated from today + frequencyDays.
     */
    @Transactional
    public RecurringExpense create(RecurringExpense template) {
        if (template.getNextOccurrenceDate() == null) {
            template.setNextOccurrenceDate(LocalDate.now().plusDays(template.getFrequencyDays()));
        }
        template.setActive(true);
        return recurringRepository.save(template);
    }

    /**
     * Finds all active templates for a user (or guests if user == null).
     */
    public List<RecurringExpense> findActiveByUser(User user) {
        if (user == null) {
            return recurringRepository.findByUserIsNullAndActiveTrue();
        }
        return recurringRepository.findByUserAndActiveTrue(user);
    }

    /**
     * Deactivates a template (used when the user unchecks "Repeat").
     */
    @Transactional
    public void deactivate(Long id, User currentUser) {
        if (id == null)
            return;

        recurringRepository.findById(id).ifPresent(template -> {
            if (currentUser == null && template.getUser() != null)
                return;
            if (currentUser != null && (template.getUser() == null
                    || !template.getUser().getId().equals(currentUser.getId()))) {
                return;
            }
            template.setActive(false);
            recurringRepository.save(template);
        });
    }

    /**
     * The core generation logic.
     * Finds every active template whose nextOccurrenceDate is today or earlier,
     * creates a real Expense from it, then advances the nextOccurrenceDate.
     */
    @Transactional
    public int generateDueExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpense> due = recurringRepository
                .findByActiveTrueAndNextOccurrenceDateLessThanEqual(today);

        int created = 0;
        for (RecurringExpense template : due) {
            Expense expense = new Expense(
                    template.getAmount(),
                    template.getNextOccurrenceDate(),
                    template.getDescription(),
                    template.getCategory(),
                    template.getUser());
            expenseRepository.save(expense);

            LocalDate next = template.getNextOccurrenceDate()
                    .plusDays(template.getFrequencyDays());
            template.setNextOccurrenceDate(next);
            recurringRepository.save(template);

            created++;
        }
        return created;
    }
}
