package com.SpringBootMVC.ExpensesTracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:frontend-api;MODE=MySQL;NON_KEYWORDS=USER"
})
@AutoConfigureMockMvc
class FrontendApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void csrfEndpointIssuesATokenForTheStaticFrontend() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registrationLoginAndExpenseApiUseTheAuthenticatedSession() throws Exception {
        String username = "frontend-api-user";
        String password = "FrontendApi!2026";
        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "firstName": "Front",
                                  "lastName": "End",
                                  "email": "frontend-api@example.invalid"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/authenticateTheUser")
                        .with(csrf())
                        .header("Accept", "application/json")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.firstName").value("Front"));

        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 2500,
                                  "category": "groceries",
                                  "dateTime": "2026-10-13T18:30",
                                  "description": "Weekly shop"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode expenses = objectMapper.readTree(
                mockMvc.perform(get("/api/expenses").session(session))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertEquals(1, expenses.size());
        int expenseId = expenses.get(0).get("id").asInt();
        assertEquals(2500, expenses.get(0).get("amount").asInt());

        mockMvc.perform(put("/api/expenses/" + expenseId)
                        .with(csrf())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 2750,
                                  "category": "groceries",
                                  "dateTime": "2026-10-13T18:30",
                                  "description": "Updated weekly shop"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/expenses/" + expenseId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void expenseApiRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in to continue."));
    }
}
