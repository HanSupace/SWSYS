package com.daily.lastsys.controller;

import com.daily.lastsys.dto.LoginForm;
import com.daily.lastsys.dto.LoginUser;
import com.daily.lastsys.service.LoginService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    public static final String LOGIN_USER_SESSION_KEY = "loginUser";

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public String loginForm(@ModelAttribute LoginForm loginForm) {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute LoginForm loginForm,
            BindingResult bindingResult,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            LoginUser loginUser = loginService.login(loginForm);
            session.setAttribute(LOGIN_USER_SESSION_KEY, loginUser);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("login.failed", exception.getMessage());
            return "auth/login";
        }

        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
