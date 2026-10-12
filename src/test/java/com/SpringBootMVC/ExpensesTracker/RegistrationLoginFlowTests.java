package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:registration;MODE=MySQL;NON_KEYWORDS=USER"
})
@AutoConfigureMockMvc
class RegistrationLoginFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersLogsInAndOpensProtectedExpenseList() throws Exception {
        String username = "flow-" + UUID.randomUUID();
        String password = "SmokeTest!2026";

        mockMvc.perform(post("/processRegistration")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password)
                        .param("firstName", "Flow")
                        .param("lastName", "Test")
                        .param("email", username + "@example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/showLoginPage?registrationSuccess"));

        User user = userRepository.findByUserName(username);
        assertNotNull(user);
        assertTrue(passwordEncoder.matches(password, user.getPassword()));
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().stream().anyMatch(role -> "ROLE_STANDARD".equals(role.getName())));

        MvcResult loginResult = mockMvc.perform(post("/authenticateTheUser")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/list"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/list").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("list-page"));
    }
}
