package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.PasswordResetToken;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.repository.PasswordResetTokenRepository;
import com.SpringBootMVC.ExpensesTracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String publicBaseUrl;
    private final String mailUsername;
    private final boolean emailDeliveryConfigured;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.public-base-url:http://localhost:8081}") String publicBaseUrl,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.publicBaseUrl = publicBaseUrl;
        this.mailUsername = mailUsername;
        this.emailDeliveryConfigured = StringUtils.hasText(mailUsername)
                && StringUtils.hasText(mailPassword)
                && mailSenderProvider.getIfAvailable() != null;
    }

    public boolean isEmailDeliveryConfigured() {
        return emailDeliveryConfigured;
    }

    @Transactional
    public void sendResetLink(String username) {
        if (!emailDeliveryConfigured) {
            throw new MailSendException("SMTP email delivery is not configured");
        }
        User user = userRepository.findByUserName(username);
        if (user == null || user.getClient() == null
                || !StringUtils.hasText(user.getClient().getEmail())) {
            return;
        }

        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);

        tokenRepository.deleteByUser_Id(user.getId());
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setUser(user);
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_LIFETIME));
        tokenRepository.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailUsername)) {
            message.setFrom(mailUsername);
        }
        message.setTo(user.getClient().getEmail());
        message.setSubject("Reset your Expenses Tracker password");
        message.setText("Use this one-time link within 30 minutes to reset your password:\n\n"
                + publicBaseUrl + "/reset-password?token=" + rawToken
                + "\n\nIf you did not request this, you can ignore this email.");
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new MailSendException("SMTP email delivery is not configured");
        }
        mailSender.send(message);
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (!StringUtils.hasText(rawToken)) {
            return false;
        }
        PasswordResetToken resetToken = tokenRepository.findByTokenHashAndUsedFalse(hashToken(rawToken));
        if (resetToken == null || !resetToken.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }

        resetToken.setUsed(true);
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        tokenRepository.save(resetToken);
        userRepository.save(user);
        return true;
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
