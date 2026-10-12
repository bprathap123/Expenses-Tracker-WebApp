package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("emailConfigured", passwordResetService.isEmailDeliveryConfigured());
        return "forgot-password";
    }

    @PostMapping("/password-reset")
    public String requestPasswordReset(
            @RequestParam("username") String username,
            RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.sendResetLink(username);
            redirectAttributes.addFlashAttribute(
                    "resetMessage",
                    "If an account matches that username, a password reset link has been sent to its email address.");
        } catch (MailException exception) {
            logger.error("Password reset email delivery failed; verify the configured SMTP service.");
        }
        redirectAttributes.addFlashAttribute(
                "resetMessage",
                "If the username matches an account, we’ll email a reset link. If no email arrives, try again later.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("resetError", "Use a password with at least 8 characters.");
            return "reset-password";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("resetError", "The passwords do not match.");
            return "reset-password";
        }
        if (!passwordResetService.resetPassword(token, password)) {
            model.addAttribute("resetError", "This reset link is invalid, expired, or already used.");
            return "reset-password";
        }

        redirectAttributes.addFlashAttribute("resetMessage", "Your password has been changed. You can sign in now.");
        return "redirect:/showLoginPage";
    }
}
