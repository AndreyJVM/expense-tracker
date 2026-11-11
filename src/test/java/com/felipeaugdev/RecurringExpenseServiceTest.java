package com.felipeaugdev;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class RecurringExpenseServiceTest {

    @Mock
    private RecurringExpenseRepository recurringRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User(1L, "john", "password", "USER");
        category = new Category("UTILITIES");
        category.setId(5L);
    }

    @Test
    void testCreateCalculatesNextOccurrenceDateWhenNull() {
        RecurringExpense template = new RecurringExpense(
                new BigDecimal("50.00"), "Internet", category, 30, null, user);
        when(recurringRepository.save(template)).thenReturn(template);

        RecurringExpense created = recurringExpenseService.create(template);

        assertNotNull(created.getNextOccurrenceDate());
        assertEquals(LocalDate.now().plusDays(30), created.getNextOccurrenceDate());
        assertTrue(created.isActive());
        verify(recurringRepository).save(template);
    }

    @Test
    void testCreatePreservesExistingNextOccurrenceDate() {
        LocalDate customDate = LocalDate.now().plusDays(10);
        RecurringExpense template = new RecurringExpense(
                new BigDecimal("50.00"), "Internet", category, 30, customDate, user);
        when(recurringRepository.save(template)).thenReturn(template);

        RecurringExpense created = recurringExpenseService.create(template);

        assertEquals(customDate, created.getNextOccurrenceDate());
        assertTrue(created.isActive());
        verify(recurringRepository).save(template);
    }

    @Test
    void testFindActiveByUserForGuestAndRegisteredUser() {
        recurringExpenseService.findActiveByUser(null);
        verify(recurringRepository).findByUserIsNullAndActiveTrue();

        recurringExpenseService.findActiveByUser(user);
        verify(recurringRepository).findByUserAndActiveTrue(user);
    }

    @Test
    void testDeactivateSuccessWhenOwnerMatches() {
        RecurringExpense template = new RecurringExpense(
                new BigDecimal("15.00"), "Streaming", category, 30, LocalDate.now(), user);
        template.setId(10L);
        when(recurringRepository.findById(10L)).thenReturn(Optional.of(template));

        recurringExpenseService.deactivate(10L, user);

        assertFalse(template.isActive());
        verify(recurringRepository).save(template);
    }

    @Test
    void testDeactivateIgnoredWhenUserDoesNotMatch() {
        User otherUser = new User(2L, "jane", "password", "USER");
        RecurringExpense template = new RecurringExpense(
                new BigDecimal("15.00"), "Streaming", category, 30, LocalDate.now(), otherUser);
        template.setId(10L);
        when(recurringRepository.findById(10L)).thenReturn(Optional.of(template));

        recurringExpenseService.deactivate(10L, user);

        assertTrue(template.isActive());
        verify(recurringRepository, never()).save(template);
    }

    @Test
    void testGenerateDueExpensesCreatesExpenseAndAdvancesDate() {
        LocalDate today = LocalDate.now();
        RecurringExpense template = new RecurringExpense(
                new BigDecimal("100.00"), "Gym", category, 30, today, user);
        template.setId(15L);

        when(recurringRepository.findByActiveTrueAndNextOccurrenceDateLessThanEqual(today))
                .thenReturn(List.of(template));

        int generatedCount = recurringExpenseService.generateDueExpenses();

        assertEquals(1, generatedCount);

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        Expense createdExpense = expenseCaptor.getValue();

        assertEquals(new BigDecimal("100.00"), createdExpense.getAmount());
        assertEquals("Gym", createdExpense.getDescription());
        assertEquals(category, createdExpense.getCategory());
        assertEquals(user, createdExpense.getUser());
        assertEquals(today, createdExpense.getDate());
        assertEquals(template, createdExpense.getRecurringExpense());

        assertEquals(today.plusDays(30), template.getNextOccurrenceDate());
        verify(recurringRepository).save(template);
    }
}
