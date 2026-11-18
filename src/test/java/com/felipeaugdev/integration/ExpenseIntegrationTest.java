package com.felipeaugdev.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@SuppressWarnings("null")
class ExpenseIntegrationTest extends AbstractIntegrationTest {

    @Test
    void userOnlySeeTheirOwnExpenses() throws Exception {
        String alice = registerAndLogin("alice");
        String bob = registerAndLogin("bob");

        createExpense(alice, "Alice lunch", "FOOD", 12.50);
        createExpense(alice, "Alice bus", "TRANSPORTATION", 3.00);
        createExpense(bob, "Bob groceries", "FOOD", 40.00);

        getExpenses(alice, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        getExpenses(bob, "")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Bob groceries"));
    }

    @Test
    void twoUsersCanUseTheSameCustomCategoryName() throws Exception {
        String alice = registerAndLogin("alice");
        String bob = registerAndLogin("bob");

        createCategory(alice, "Gym").andExpect(status().isCreated());
        createCategory(bob, "Gym").andExpect(status().isCreated());

        createExpense(alice, "Alice gym fee", "GYM", 30.00);
        createExpense(bob, "Bob gym fee", "GYM", 45.00);

        getExpenses(alice, "")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Alice gym fee"))
                .andExpect(jsonPath("$.content[0].category").value("GYM"));
        getExpenses(bob, "")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Bob gym fee"))
                .andExpect(jsonPath("$.content[0].category").value("GYM"));
    }

    @Test
    void deleteOnlyWorksOnYourOwnExpenses() throws Exception {
        String alice = registerAndLogin("alice");
        String bob = registerAndLogin("bob");
        int id = createExpense(alice, "Alice lunch", "FOOD", 12.50).get("id").asInt();

        mockMvc.perform(delete("/api/expenses/" + id).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
        getExpenses(alice, "").andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/expenses/" + id).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        getExpenses(alice, "").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void guestExpensesAreSeparateFromUserExpenses() throws Exception {
        String alice = registerAndLogin("alice");
        String guestDescription = "Guest coffee " + UUID.randomUUID();
        createExpense(null, guestDescription, "FOOD", 4.00);

        getExpenses(null, "size=100")
                .andExpect(jsonPath("$.content[*].description", hasItem(guestDescription)));
        getExpenses(alice, "size=100")
                .andExpect(jsonPath("$.content[*].description", not(hasItem(guestDescription))));
    }

    @Test
    void invalidExpenseIsRejectedWithBadRequest() throws Exception {
        String alice = registerAndLogin("alice");

        mockMvc.perform(post("/api/expenses")
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("amount", -5, "description", "Oops", "category", "FOOD"))))
                .andExpect(status().isBadRequest());
    }
}
