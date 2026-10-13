package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.service.ClientService;
import com.SpringBootMVC.ExpensesTracker.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class CustomeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    UserService userService;
    ClientService clientService;

    @Autowired
    public CustomeAuthenticationSuccessHandler(UserService userService, ClientService clientService) {
        this.userService = userService;
        this.clientService = clientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response
            , Authentication authentication) throws IOException, ServletException {
        Client client;
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            Object clientId = oauth2User.getAttribute("appClientId");
            if (!(clientId instanceof Number)) {
                throw new IllegalStateException("OAuth-authenticated user has no associated client");
            }
            client = clientService.findClientById(((Number) clientId).intValue());
        } else {
            User user = userService.findUserByUserName(authentication.getName());
            client = user == null ? null : user.getClient();
        }
        if (client == null) {
            throw new IllegalStateException("Authenticated user has no associated client");
        }
        HttpSession session = request.getSession();
        session.setAttribute("client", client);
        if (request.getHeader("Accept") != null
                && request.getHeader("Accept").contains("application/json")) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.sendRedirect(request.getContextPath() + "/list");
        }
    }
}
