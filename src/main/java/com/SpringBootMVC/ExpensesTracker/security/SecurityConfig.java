package com.SpringBootMVC.ExpensesTracker.security;

import com.SpringBootMVC.ExpensesTracker.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService){
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http
            , AuthenticationSuccessHandler customAuthenticationSuccessHandler,
            ExpenseTrackerOAuth2UserService oauth2UserService) throws Exception{
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        http.csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(config ->
                config
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/api/csrf", "/api/session", "/api/config", "/api/register").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/showRegistrationForm").permitAll()
                        .requestMatchers("/processRegistration").permitAll()
                        .requestMatchers("/forgot-password", "/password-reset", "/reset-password").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Please sign in to continue.\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/showLoginPage");
                    }
                }))
                .formLogin(form ->
                        form
                                .loginPage("/showLoginPage")
                                .loginProcessingUrl("/authenticateTheUser")
                                .successHandler(customAuthenticationSuccessHandler)
                                .failureHandler((request, response, exception) -> {
                                    if (request.getHeader("Accept") != null
                                            && request.getHeader("Accept").contains("application/json")) {
                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                        response.setContentType("application/json");
                                        response.getWriter().write("{\"message\":\"Username or password is incorrect.\"}");
                                    } else {
                                        new SimpleUrlAuthenticationFailureHandler("/showLoginPage?error")
                                                .onAuthenticationFailure(request, response, exception);
                                    }
                                })
                                .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/showLoginPage")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler(customAuthenticationSuccessHandler))
                .logout(logout ->
                        logout
                                .permitAll()
                                .logoutUrl("/logout")
                                .logoutSuccessHandler((request, response, authentication) -> {
                                    if (request.getHeader("Accept") != null
                                            && request.getHeader("Accept").contains("application/json")) {
                                        response.setStatus(HttpStatus.NO_CONTENT.value());
                                    } else {
                                        response.sendRedirect(request.getContextPath() + "/showLoginPage");
                                    }
                                }));
        return http.build();
    }

}
