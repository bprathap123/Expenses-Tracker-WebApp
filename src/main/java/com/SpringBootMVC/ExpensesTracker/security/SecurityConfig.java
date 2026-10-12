package com.SpringBootMVC.ExpensesTracker.security;

import com.SpringBootMVC.ExpensesTracker.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

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
        http.authorizeHttpRequests(config ->
                config
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/showRegistrationForm").permitAll()
                        .requestMatchers("/processRegistration").permitAll()
                        .requestMatchers("/forgot-password", "/password-reset", "/reset-password").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form ->
                        form
                                .loginPage("/showLoginPage")
                                .loginProcessingUrl("/authenticateTheUser")
                                .successHandler(customAuthenticationSuccessHandler)
                                .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/showLoginPage")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler(customAuthenticationSuccessHandler))
                .logout(logout ->
                        logout
                                .permitAll()
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/showLoginPage"));
        return http.build();
    }

}
