package com.SpringBootMVC.ExpensesTracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String showLandingPage() {
        return "landing-page";
    }

    @GetMapping("/showLoginPage")
    public String showLoginPage(){
        return "login-page";
    }
}
