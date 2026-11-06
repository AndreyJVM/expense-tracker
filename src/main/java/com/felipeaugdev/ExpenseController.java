package com.felipeaugdev;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.felipeaugdev.dto.ExpenseCreateRequest;
import com.felipeaugdev.dto.ExpenseResponse;
import com.felipeaugdev.dto.ExpenseUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Helper that returns the currently authenticated User,
     * or null when the request is from a guest.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    @GetMapping
    public Page<ExpenseResponse> getExpenses(
            @RequestParam(defaultValue = "0") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        var direction = sortDirection.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        new Sort.Order(direction, Objects.requireNonNull(sortField)),
                        Sort.Order.desc("id")));

        return expenseService.getExpenses(days, getCurrentUser(), pageable)
                .map(ExpenseResponse::from);

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseCreateRequest request) {
        Expense saved = expenseService.addExpense(
                request.getAmount(),
                request.getDescription(),
                request.getCategory(),
                getCurrentUser(),
                request.isRecurring(),
                request.getFrequencyDays());
        return ExpenseResponse.from(saved);
    }

    @PutMapping("/{id}")
    public ExpenseResponse updateExpense(
            @PathVariable int id,
            @Valid @RequestBody ExpenseUpdateRequest request) {

        Expense updated = expenseService.updateExpense(id, request, getCurrentUser());
        return ExpenseResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable int id,
            @RequestParam(defaultValue = "false") boolean stopRecurring) {

        boolean deleted = expenseService.deleteExpense(id, getCurrentUser(), stopRecurring);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/category-totals")
    public Map<String, BigDecimal> getCategoryTotals(@RequestParam(defaultValue = "0") int days) {
        return expenseService.getTotalExpensesByCategory(days, getCurrentUser());
    }

    @GetMapping("/monthly-report")
    public MonthOverMonthReport getMonthlyReport() {
        return expenseService.getMonthOverMonthReport(getCurrentUser());
    }

}
