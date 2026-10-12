package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Role;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.repository.RoleRepository;
import com.SpringBootMVC.ExpensesTracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-reset;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.mail.host=smtp.example.invalid",
        "spring.mail.username=sender@example.invalid",
        "spring.mail.password=test-only-password"
})
@AutoConfigureMockMvc
class PasswordResetFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.SpringBootMVC.ExpensesTracker.service.PasswordResetService passwordResetService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void sendsOneTimeResetLinkAndChangesPassword() throws Exception {
        String username = "reset-flow-user";
        User user = createUser(username);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        mockMvc.perform(post("/password-reset")
                        .with(csrf())
                        .param("username", username))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String emailBody = messageCaptor.getValue().getText();
        assertNotNull(emailBody);
        String token = emailBody.substring(emailBody.indexOf("token=") + "token=".length())
                .split("\\s", 2)[0];

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"));

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", token)
                        .param("password", "ChangedPassword2026!")
                        .param("confirmPassword", "ChangedPassword2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/showLoginPage"));

        User updatedUser = userRepository.findByUserName(username);
        assertTrue(passwordEncoder.matches("ChangedPassword2026!", updatedUser.getPassword()));
        assertFalse(passwordResetService.resetPassword(token, "AnotherPassword2026!"));
    }

    private User createUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setPassword(passwordEncoder.encode("OldPassword2026!"));
        user.setEnabled(true);
        user.setClient(new Client("Reset", "Test", username + "@example.invalid"));
        Role role = roleRepository.findByName("ROLE_STANDARD");
        user.setRoles(java.util.List.of(role));
        return userRepository.save(user);
    }
}
