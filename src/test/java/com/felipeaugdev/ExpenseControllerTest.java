package com.felipeaugdev;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipeaugdev.dto.ExpenseCreateRequest;
import com.felipeaugdev.security.JwtUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ExpenseControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ExpenseService expenseService;

        @MockBean
        private JwtUtils jwtUtils;

        @MockBean
        private UserDetailsService userDetailsService;

        private final Category food = new Category("FOOD");

        @Test
        void testGetExpensesReturnsJsonList() throws Exception {
                Expense expense = new Expense(new BigDecimal("15.00"), LocalDate.now(), "Coffee", food);

                Page<Expense> page = new PageImpl<>(List.of(expense));

                when(expenseService.getExpenses(eq(0), any(), any(Pageable.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/expenses"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].description").value("Coffee"))
                                .andExpect(jsonPath("$.content[0].amount").value(15.00));
        }

        @Test
        void testAddExpenseReturnsCreatedStatus() throws Exception {
                Expense expense = new Expense(new BigDecimal("25.50"), LocalDate.now(), "Dinner", food);
                when(expenseService.addExpense(any(BigDecimal.class), eq("Dinner"),
                                eq("FOOD"), any(), anyBoolean(), any()))
                                .thenReturn(expense);

                ExpenseCreateRequest request = new ExpenseCreateRequest();
                request.setAmount(new BigDecimal("25.50"));
                request.setDescription("Dinner");
                request.setCategory("FOOD");

                mockMvc.perform(post("/api/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.description").value("Dinner"));
        }

        @Test
        void testDeleteExpenseExistingReturnsNoContent() throws Exception {
                when(expenseService.deleteExpense(eq(1), any())).thenReturn(true);

                mockMvc.perform(delete("/api/expenses/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void testDeleteExpenseNotFoundReturns404() throws Exception {
                when(expenseService.deleteExpense(eq(999), any())).thenReturn(false);

                mockMvc.perform(delete("/api/expenses/999"))
                                .andExpect(status().isNotFound());
        }
}
