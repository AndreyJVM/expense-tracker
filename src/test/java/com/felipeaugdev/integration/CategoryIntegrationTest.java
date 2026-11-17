package com.felipeaugdev.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@SuppressWarnings("null")
class CategoryIntegrationTest extends AbstractIntegrationTest {

    private ResultActions createCategory(String token, String name) throws Exception {
        var request = post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    @Test
    void twoUsersCanCreateCategoriesWithTheSameName() throws Exception {
        String alice = registerAndLogin("alice");
        String bob = registerAndLogin("bob");

        createCategory(alice, "Gym")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("GYM"));
        createCategory(bob, "Gym")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("GYM"));
    }

    @Test
    void sameUserCannotCreateTheSameCategoryTwice() throws Exception {
        String alice = registerAndLogin("alice");

        createCategory(alice, "Hobby").andExpect(status().isCreated());
        createCategory(alice, "hobby")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a category with this name"));
    }

    @Test
    void systemCategoryNamesAreReserved() throws Exception {
        String alice = registerAndLogin("alice");

        createCategory(alice, "food")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This category name is reserved"));
    }

    @Test
    void guestsCannotCreateCategories() throws Exception {
        createCategory(null, "Gym").andExpect(status().isUnauthorized());
    }
}
