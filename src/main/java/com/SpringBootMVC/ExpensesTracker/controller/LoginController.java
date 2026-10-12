package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.service.PasswordResetService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final ClientRegistrationRepository registrations;
    private final PasswordResetService passwordResetService;

    public LoginController(
            ClientRegistrationRepository registrations,
            PasswordResetService passwordResetService) {
        this.registrations = registrations;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/")
    public String showLandingPage() {
        return "landing-page";
    }

    @GetMapping("/showLoginPage")
    public String showLoginPage(Model model) {
        model.addAttribute("googleLoginEnabled", registrations.findByRegistrationId("google") != null);
        model.addAttribute("facebookLoginEnabled", registrations.findByRegistrationId("facebook") != null);
        model.addAttribute("passwordResetEnabled", passwordResetService.isEmailDeliveryConfigured());
        return "login-page";
    }
}
