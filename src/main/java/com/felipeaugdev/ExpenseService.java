package com.felipeaugdev;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.felipeaugdev.dto.ExpenseUpdateRequest;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final CategoryRepository categoryRepository;
    private final RecurringExpenseService recurringExpenseService;

    public ExpenseService(ExpenseRepository repository,
            CategoryRepository categoryRepository,
            RecurringExpenseService recurringExpenseService) {

        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.recurringExpenseService = recurringExpenseService;
    }

    public Expense addExpense(BigDecimal amount, String description, String categoryName,
            User user, boolean recurring, Integer frequencyDays) {

        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));

        Expense newExpense = new Expense(amount, LocalDate.now(), description, category, user);
        Expense saved = repository.save(newExpense);

        if (recurring && frequencyDays != null && frequencyDays > 0) {
            RecurringExpense template = new RecurringExpense(
                    amount,
                    description,
                    category,
                    frequencyDays,
                    LocalDate.now().plusDays(frequencyDays),
                    user);
            template = recurringExpenseService.create(template);

            saved.setRecurringExpense(template);
            saved = repository.save(saved);
        }

        return saved;
    }

    public List<Expense> getAllExpenses(User user) {
        List<Expense> all = repository.findAll();
        return filterByUser(all, user);
    }

    public Page<Expense> getExpenses(int days, User user, Pageable pageable) {
        List<Expense> filtered = getExpensesByDateRange(days, user);
        filtered = sortExpenses(filtered, pageable);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<Expense> pageContent = start >= filtered.size()
                ? List.of()
                : filtered.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
                Objects.requireNonNull(pageContent),
                pageable,
                filtered.size());
    }

    public Expense updateExpense(int id, ExpenseUpdateRequest request, User currentUser) {
        Expense expense = repository.findById(id)
                .filter(e -> belongsToUser(e, currentUser))
                .orElseThrow(() -> new RuntimeException("Expense not found or access denied"));

        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getDate() != null) {
            expense.setDate(request.getDate());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findByName(request.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategory()));

            expense.setCategory(category);
        }

        if (request.getRecurring() != null) {
            if (request.getRecurring()) {
                int freq = request.getFrequencyDays() != null ? request.getFrequencyDays() : 30;

                RecurringExpense template = expense.getRecurringExpense();
                if (template == null) {
                    template = new RecurringExpense(
                            expense.getAmount(),
                            expense.getDescription(),
                            expense.getCategory(),
                            freq,
                            LocalDate.now().plusDays(freq),
                            currentUser);
                    template = recurringExpenseService.create(template);
                    expense.setRecurringExpense(template);
                } else {
                    template.setFrequencyDays(freq);
                    template.setActive(true);
                    template.setAmount(expense.getAmount());
                    template.setDescription(expense.getDescription());
                    template.setCategory(expense.getCategory());

                    recurringExpenseService.update(template);
                }
            } else {
                if (expense.getRecurringExpense() != null) {
                    recurringExpenseService.deactivate(
                            expense.getRecurringExpense().getId(), currentUser);
                    expense.setRecurringExpense(null);
                }
            }
        }

        return repository.save(Objects.requireNonNull(expense));
    }

    public boolean deleteExpense(int id, User user, boolean stopRecurring) {
        return repository.findById(id)
                .filter(expense -> belongsToUser(expense, user))
                .map(expense -> {
                    if (stopRecurring && expense.getRecurringExpense() != null) {
                        recurringExpenseService.deactivate(expense.getRecurringExpense().getId(), user);
                    }
                    repository.delete(Objects.requireNonNull(expense));
                    return true;
                })
                .orElse(false);
    }

    /**
     * Calculates total expenses grouped by category for a given date range.
     * 
     * @param days Number of days back to include (0 for All Time).
     * @return Map where the key is the category name and the value is the total amount.
     */
    public Map<String, BigDecimal> getTotalExpensesByCategory(int days, User user) {
        List<Expense> filtered = getExpensesByDateRange(days, user);
        Map<String, BigDecimal> categoryTotals = new TreeMap<>();

        for (Expense expense : filtered) {
            String categoryName = expense.getCategory().getName();
            BigDecimal amount = expense.getAmount();
            categoryTotals.merge(categoryName, amount, (a, b) -> a.add(b));
        }
        return categoryTotals;
    }

    /**
     * Calculates the grand total of all expenses for a given time range.
     * 
     * @param days Number of days back to include (0 for All Time).
     * @return BigDecimal total of all expenses within the time range.
     */
    public BigDecimal getTotalExpenses(int days, User user) {
        return getExpensesByDateRange(days, user).stream()
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    /**
     * Retrieves expenses filtered by date range.
     * 
     * @param days Number of days back to filter (7, 14, 30). Pass 0 for All Time.
     * @return List of matching Expense objects.
     */
    public List<Expense> getExpensesByDateRange(int days, User user) {
        List<Expense> all = getAllExpenses(user);

        if (days <= 0) {
            return all;
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return all.stream()
                .filter(e -> !e.getDate().isBefore(cutoffDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculates total expenses for a specific calendar month.
     */
    public BigDecimal getTotalExpensesForMonth(YearMonth yearMonth, User user) {
        return getAllExpenses(user).stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(yearMonth))
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    /**
     * Generates a Month-over-Month report comparing the current month against the previous month.
     */
    public MonthOverMonthReport getMonthOverMonthReport(User user) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        BigDecimal currentTotal = getTotalExpensesForMonth(currentMonth, user);
        BigDecimal previousTotal = getTotalExpensesForMonth(previousMonth, user);

        BigDecimal difference = currentTotal.subtract(previousTotal);

        BigDecimal percentageChange = BigDecimal.ZERO;
        if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = difference
                    .divide(previousTotal, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new MonthOverMonthReport(previousMonth, currentMonth,
                previousTotal, currentTotal,
                difference, percentageChange);
    }

    private List<Expense> filterByUser(List<Expense> expenses, User user) {
        if (user == null) {
            return expenses.stream()
                    .filter(e -> e.getUser() == null)
                    .collect(Collectors.toList());
        }
        return expenses.stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }

    private boolean belongsToUser(Expense expense, User user) {
        if (user == null) {
            return expense.getUser() == null;
        }
        return expense.getUser() != null && expense.getUser().getId().equals(user.getId());
    }

    private List<Expense> sortExpenses(List<Expense> expenses, Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return expenses;
        }

        return expenses.stream()
                .sorted((e1, e2) -> {
                    for (var order : pageable.getSort()) {
                        int comparison = switch (order.getProperty()) {
                            case "date" -> e1.getDate().compareTo(e2.getDate());
                            case "amount" -> e1.getAmount().compareTo(e2.getAmount());
                            case "description" -> e1.getDescription().compareToIgnoreCase(e2.getDescription());
                            case "category" ->
                                e1.getCategory().getName().compareToIgnoreCase(e2.getCategory().getName());
                            case "id" -> e1.getId().compareTo(e2.getId());
                            default -> 0;
                        };

                        if (comparison != 0) {
                            return order.isAscending() ? comparison : -comparison;
                        }
                    }
                    return 0;
                })
                .toList();
    }
}
