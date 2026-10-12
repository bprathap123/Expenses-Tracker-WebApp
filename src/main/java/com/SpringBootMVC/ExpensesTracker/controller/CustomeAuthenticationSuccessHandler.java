package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class CustomeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    UserService userService;

    @Autowired
    public CustomeAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response
            , Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        User user = userService.findUserByUserName(username);
        if (user == null || user.getClient() == null) {
            throw new IllegalStateException("Authenticated user has no associated client");
        }
        Client client = user.getClient();
        HttpSession session = request.getSession();
        session.setAttribute("client", client);
        response.sendRedirect(request.getContextPath()+"/list");
    }
}
