package com.felipeaugdev.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ExpenseListingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void paginationSplitsResultsIntoPages() throws Exception {
        String alice = registerAndLogin("alice");
        for (int i = 1; i <= 5; i++) {
            createExpense(alice, "Expense " + i, "FOOD", i);
        }

        getExpenses(alice, "size=2&page=0")
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        getExpenses(alice, "size=2&page=2")
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void defaultOrderIsNewestFirst() throws Exception {
        String alice = registerAndLogin("alice");
        createExpense(alice, "First", "FOOD", 5.00);
        createExpense(alice, "Second", "FOOD", 5.00);

        getExpenses(alice, "")
                .andExpect(jsonPath("$.content[0].description").value("Second"))
                .andExpect(jsonPath("$.content[1].description").value("First"));
    }

    @Test
    void canSortByAmountInBothDirections() throws Exception {
        String alice = registerAndLogin("alice");
        createExpense(alice, "Mid", "FOOD", 20.00);
        createExpense(alice, "Low", "FOOD", 10.00);
        createExpense(alice, "High", "FOOD", 30.00);

        getExpenses(alice, "sort=amount,asc")
                .andExpect(jsonPath("$.content[0].description").value("Low"))
                .andExpect(jsonPath("$.content[1].description").value("Mid"))
                .andExpect(jsonPath("$.content[2].description").value("High"));
        getExpenses(alice, "sort=amount,desc")
                .andExpect(jsonPath("$.content[0].description").value("High"))
                .andExpect(jsonPath("$.content[2].description").value("Low"));
    }

    @Test
    void canSortByCategoryName() throws Exception {
        String alice = registerAndLogin("alice");
        createExpense(alice, "Taxi", "TRANSPORTATION", 15.00);
        createExpense(alice, "Lunch", "FOOD", 10.00);
        createExpense(alice, "Power", "UTILITIES", 60.00);

        getExpenses(alice, "sort=category,asc")
                .andExpect(jsonPath("$.content[0].category").value("FOOD"))
                .andExpect(jsonPath("$.content[1].category").value("TRANSPORTATION"))
                .andExpect(jsonPath("$.content[2].category").value("UTILITIES"));
    }

    @Test
    @SuppressWarnings("null")
    void daysFilterExcludesOlderExpenses() throws Exception {
        String alice = registerAndLogin("alice");
        createExpense(alice, "Recent", "FOOD", 5.00);
        int oldId = createExpense(alice, "Old", "FOOD", 5.00).get("id").asInt();

        Map<String, Object> update = Map.of(
                "amount", 5.00,
                "date", LocalDate.now().minusDays(60).toString(),
                "description", "Old",
                "category", "FOOD");
        mockMvc.perform(put("/api/expenses/" + oldId)
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        getExpenses(alice, "days=30")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Recent"));
        getExpenses(alice, "days=0")
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
