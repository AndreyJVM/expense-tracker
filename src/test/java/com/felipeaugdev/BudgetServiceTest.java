package com.felipeaugdev;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.felipeaugdev.dto.BudgetResponse;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User(1L, "john", "password", "USER");
        category = createCategory(10L, "FOOD");
    }

    private Category createCategory(Long id, String name) {
        Category cat = new Category(name);
        cat.setId(id);
        return cat;
    }

    @Test
    void testGetBudgetsForUserReturnsEmptyWhenUserIsNull() {
        List<BudgetResponse> responses = budgetService.getBudgetsForUser(null);

        assertTrue(responses.isEmpty());
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void testGetBudgetsForUserCalculatesSpentAndRemainingCorrectly() {
        BigDecimal limit = new BigDecimal("100.00");
        BigDecimal spent = new BigDecimal("40.00");
        BigDecimal remaining = new BigDecimal("60.00");

        Budget budget = new Budget(user, category, limit);
        budget.setId(100L);

        Category transport = createCategory(20L, "TRANSPORTATION");
        Expense targetExpense = new Expense(spent, LocalDate.now(), "Groceries", category, user);
        Expense otherCategoryExpense = new Expense(new BigDecimal("20.00"), LocalDate.now(), "Bus", transport, user);

        when(budgetRepository.findByUserId(user.getId())).thenReturn(List.of(budget));
        when(expenseRepository.findAll()).thenReturn(List.of(targetExpense, otherCategoryExpense));

        List<BudgetResponse> result = budgetService.getBudgetsForUser(user);

        assertEquals(1, result.size());
        BudgetResponse response = result.get(0);
        assertEquals(budget.getId(), response.getId());
        assertEquals(category.getName(), response.getCategory());
        assertEquals(limit, response.getLimit());
        assertEquals(spent, response.getSpent());
        assertEquals(remaining, response.getRemaining());
        assertEquals(40.0, response.getPercentage());
    }

    @Test
    void testCreateOrUpdateBudgetThrowsWhenUserIsNull() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.createOrUpdateBudget(null, "FOOD", new BigDecimal("100.00"))
        );

        assertEquals("You must be logged in to set a budget", exception.getMessage());
    }

    @Test
    void testCreateOrUpdateBudgetThrowsWhenCategoryNotFound() {
        when(categoryRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.createOrUpdateBudget(user, "UNKNOWN", new BigDecimal("100.00"))
        );

        assertTrue(exception.getMessage().contains("Category not found"));
    }

    @Test
    void testCreateBudgetForNewCategory() {
        BigDecimal amount = new BigDecimal("150.00");
        Budget savedBudget = new Budget(user, category, amount);
        savedBudget.setId(200L);

        when(categoryRepository.findByName(category.getName())).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, category.getName(), amount);

        assertNotNull(response);
        assertEquals(savedBudget.getId(), response.getId());
        assertEquals(category.getName(), response.getCategory());
        assertEquals(amount, response.getLimit());
        assertEquals(BigDecimal.ZERO, response.getSpent());
        assertEquals(amount, response.getRemaining());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void testUpdateBudgetForExistingCategory() {
        BigDecimal oldAmount = new BigDecimal("100.00");
        BigDecimal newAmount = new BigDecimal("250.00");
        Budget existingBudget = new Budget(user, category, oldAmount);
        existingBudget.setId(200L);

        when(categoryRepository.findByName(category.getName())).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, category.getName(), newAmount);

        assertEquals(newAmount, response.getLimit());
        assertEquals(newAmount, existingBudget.getAmount());
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    void testDeleteBudgetSuccess() {
        Budget budget = new Budget(user, category, new BigDecimal("100.00"));
        budget.setId(300L);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(user, budget.getId());

        verify(budgetRepository).delete(budget);
    }

    @Test
    void testDeleteBudgetThrowsWhenNotFound() {
        Long nonExistentId = 999L;
        when(budgetRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, nonExistentId)
        );

        assertEquals("Budget not found", exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void testDeleteBudgetThrowsWhenAccessDenied() {
        User otherUser = new User(2L, "jane", "password", "USER");
        Budget budget = new Budget(otherUser, category, new BigDecimal("100.00"));
        budget.setId(300L);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, budget.getId())
        );

        assertEquals("Access denied", exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }
}
