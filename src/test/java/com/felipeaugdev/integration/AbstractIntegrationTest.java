package com.felipeaugdev.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipeaugdev.dto.AuthRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@SuppressWarnings("null")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Registers a brand-new user through the real API and returns their JWT. */
    protected String registerAndLogin(String usernamePrefix) throws Exception {
        String username = usernamePrefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(new AuthRequest(username, "password123"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    /** Creates a custom category. Pass a null token to act as a guest. */
    protected ResultActions createCategory(String token, String name) throws Exception {
        var request = post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    /** Creates an expense (must succeed) and returns the response JSON. Null token = guest. */
    protected JsonNode createExpense(String token, String description, String category, double amount)
            throws Exception {
        var request = post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "amount", amount,
                        "description", description,
                        "category", category)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        String response = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    /** Calls GET /api/expenses with a raw query string such as "size=2&page=1". */
    protected ResultActions getExpenses(String token, String query) throws Exception {
        var request = get("/api/expenses?" + query);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }
}
