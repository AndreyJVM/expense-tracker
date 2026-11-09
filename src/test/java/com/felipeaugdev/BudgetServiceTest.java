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

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USERNAME = "john";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String DEFAULT_ROLE = "USER";

    private static final Long OTHER_USER_ID = 2L;
    private static final String OTHER_USERNAME = "jane";

    private static final Long FOOD_CATEGORY_ID = 10L;
    private static final String FOOD_CATEGORY_NAME = "FOOD";

    private static final Long TRANSPORT_CATEGORY_ID = 20L;
    private static final String TRANSPORT_CATEGORY_NAME = "TRANSPORTATION";

    private static final String UNKNOWN_CATEGORY_NAME = "UNKNOWN";

    private static final Long BUDGET_ID = 100L;
    private static final Long NEW_BUDGET_ID = 200L;
    private static final Long NON_EXISTENT_BUDGET_ID = 999L;

    private static final BigDecimal AMOUNT_100 = new BigDecimal("100.00");
    private static final BigDecimal AMOUNT_40 = new BigDecimal("40.00");
    private static final BigDecimal AMOUNT_20 = new BigDecimal("20.00");
    private static final BigDecimal AMOUNT_60 = new BigDecimal("60.00");
    private static final BigDecimal AMOUNT_150 = new BigDecimal("150.00");
    private static final BigDecimal AMOUNT_250 = new BigDecimal("250.00");

    private static final String GROCERIES_DESCRIPTION = "Groceries";
    private static final String BUS_DESCRIPTION = "Bus";

    private static final String MSG_MUST_BE_LOGGED_IN = "You must be logged in to set a budget";
    private static final String MSG_CATEGORY_NOT_FOUND = "Category not found";
    private static final String MSG_BUDGET_NOT_FOUND = "Budget not found";
    private static final String MSG_ACCESS_DENIED = "Access denied";

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
        user = new User(DEFAULT_USER_ID, DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_ROLE);
        category = new Category(FOOD_CATEGORY_NAME);
        category.setId(FOOD_CATEGORY_ID);
    }

    @Test
    void testGetBudgetsForUserReturnsEmptyWhenUserIsNull() {
        List<BudgetResponse> responses = budgetService.getBudgetsForUser(null);
        assertTrue(responses.isEmpty());
        verifyNoInteractions(budgetRepository);
    }

    @Test
    void testGetBudgetsForUserCalculatesSpentAndRemainingCorrectly() {
        Budget budget = new Budget(user, category, AMOUNT_100);
        budget.setId(BUDGET_ID);

        Category transport = new Category(TRANSPORT_CATEGORY_NAME);
        transport.setId(TRANSPORT_CATEGORY_ID);

        Expense inCurrentMonth = new Expense(AMOUNT_40, LocalDate.now(), GROCERIES_DESCRIPTION, category, user);
        Expense differentCategory = new Expense(AMOUNT_20, LocalDate.now(), BUS_DESCRIPTION, transport, user);

        when(budgetRepository.findByUserId(user.getId())).thenReturn(List.of(budget));
        when(expenseRepository.findAll()).thenReturn(List.of(inCurrentMonth, differentCategory));

        List<BudgetResponse> result = budgetService.getBudgetsForUser(user);

        assertEquals(1, result.size());
        BudgetResponse response = result.get(0);
        assertEquals(budget.getId(), response.getId());
        assertEquals(category.getName(), response.getCategory());
        assertEquals(AMOUNT_100, response.getLimit());
        assertEquals(AMOUNT_40, response.getSpent());
        assertEquals(AMOUNT_60, response.getRemaining());
        assertEquals(40.0, response.getPercentage());
    }

    @Test
    void testCreateOrUpdateBudgetThrowsWhenUserIsNull() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.createOrUpdateBudget(null, FOOD_CATEGORY_NAME, AMOUNT_100)
        );
        assertEquals(MSG_MUST_BE_LOGGED_IN, exception.getMessage());
    }

    @Test
    void testCreateOrUpdateBudgetThrowsWhenCategoryNotFound() {
        when(categoryRepository.findByName(UNKNOWN_CATEGORY_NAME)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.createOrUpdateBudget(user, UNKNOWN_CATEGORY_NAME, AMOUNT_100)
        );
        assertTrue(exception.getMessage().contains(MSG_CATEGORY_NOT_FOUND));
    }

    @Test
    void testCreateBudgetForNewCategory() {
        when(categoryRepository.findByName(category.getName())).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(Optional.empty());

        Budget savedBudget = new Budget(user, category, AMOUNT_150);
        savedBudget.setId(NEW_BUDGET_ID);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, category.getName(), AMOUNT_150);

        assertNotNull(response);
        assertEquals(savedBudget.getId(), response.getId());
        assertEquals(category.getName(), response.getCategory());
        assertEquals(AMOUNT_150, response.getLimit());
        assertEquals(BigDecimal.ZERO, response.getSpent());
        assertEquals(AMOUNT_150, response.getRemaining());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void testUpdateBudgetForExistingCategory() {
        Budget existingBudget = new Budget(user, category, AMOUNT_100);
        existingBudget.setId(NEW_BUDGET_ID);

        when(categoryRepository.findByName(category.getName())).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);
        when(expenseRepository.findAll()).thenReturn(List.of());

        BudgetResponse response = budgetService.createOrUpdateBudget(user, category.getName(), AMOUNT_250);

        assertEquals(AMOUNT_250, response.getLimit());
        assertEquals(AMOUNT_250, existingBudget.getAmount());
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    void testDeleteBudgetSuccess() {
        Budget budget = new Budget(user, category, AMOUNT_100);
        budget.setId(BUDGET_ID);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(user, budget.getId());

        verify(budgetRepository).delete(budget);
    }

    @Test
    void testDeleteBudgetThrowsWhenNotFound() {
        when(budgetRepository.findById(NON_EXISTENT_BUDGET_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, NON_EXISTENT_BUDGET_ID)
        );
        assertEquals(MSG_BUDGET_NOT_FOUND, exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void testDeleteBudgetThrowsWhenAccessDenied() {
        User otherUser = new User(OTHER_USER_ID, OTHER_USERNAME, DEFAULT_PASSWORD, DEFAULT_ROLE);
        Budget budget = new Budget(otherUser, category, AMOUNT_100);
        budget.setId(BUDGET_ID);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                budgetService.deleteBudget(user, budget.getId())
        );
        assertEquals(MSG_ACCESS_DENIED, exception.getMessage());
        verify(budgetRepository, never()).delete(any());
    }
}
