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
        category = new Category("FOOD");
        category.setId(10L);
    }

    @Test
    void testGetBudgetsForUserReturnsEmptyWhenUserIsNull() {
        List<BudgetResponse> responses = budgetService.getBudgetsForUser(null);
        assertTrue(responses.isEmpty());
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void testGetBudgetsForUserCalculatesSpentAndRemainingCorrectly() {
        Budget budget = new Budget(user, category, new BigDecimal("100.00"));
        budget.setId(100L);

        Category transport = new Category("TRANSPORTATION");
        transport.setId(20L);

        Expense inCurrentMonth = new Expense(new BigDecimal("40.00"), LocalDate.now(), "Groceries", category, user);
        Expense differentCategory = new Expense(new BigDecimal("20.00"), LocalDate.now(), "Bus", transport, user);

        when(budgetRepository.findByUserId(1L)).thenReturn(List.of(budget));
        when(expenseRepository.findAll()).thenReturn(List.of(inCurrentMonth, differentCategory));

        List<BudgetResponse> result = budgetService.getBudgetsForUser(user);

        assertEquals(1, result.size());
        BudgetResponse response = result.get(0);
        assertEquals(100L, response.getId());
        assertEquals("FOOD", response.getCategory());
        assertEquals(new BigDecimal("100.00"), response.getLimit());
        assertEquals(new BigDecimal("40.00"), response.getSpent());
        assertEquals(new BigDecimal("60.00"), response.getRemaining());
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
        when(categoryRepository.findByName("FOOD")).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(1L, 10L)).thenReturn(Optional.empty());

        Budget savedBudget = new Budget(user, category, new BigDecimal("150.00"));
        savedBudget.setId(200L);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, "FOOD", new BigDecimal("150.00"));

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("FOOD", response.getCategory());
        assertEquals(new BigDecimal("150.00"), response.getLimit());
        assertEquals(BigDecimal.ZERO, response.getSpent());
        assertEquals(new BigDecimal("150.00"), response.getRemaining());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void testUpdateBudgetForExistingCategory() {
        Budget existingBudget = new Budget(user, category, new BigDecimal("100.00"));
        existingBudget.setId(200L);

        when(categoryRepository.findByName("FOOD")).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(1L, 10L)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, "FOOD", new BigDecimal("250.00"));

        assertEquals(new BigDecimal("250.00"), response.getLimit());
        assertEquals(new BigDecimal("250.00"), existingBudget.getAmount());
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    void testDeleteBudgetSuccess() {
        Budget budget = new Budget(user, category, new BigDecimal("100.00"));
        budget.setId(300L);

        when(budgetRepository.findById(300L)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(user, 300L);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void testDeleteBudgetThrowsWhenNotFound() {
        when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, 999L)
        );
        assertEquals("Budget not found", exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void testDeleteBudgetThrowsWhenAccessDenied() {
        User otherUser = new User(2L, "jane", "password", "USER");
        Budget budget = new Budget(otherUser, category, new BigDecimal("100.00"));
        budget.setId(300L);

        when(budgetRepository.findById(300L)).thenReturn(Optional.of(budget));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, 300L)
        );
        assertEquals("Access denied", exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }
}
