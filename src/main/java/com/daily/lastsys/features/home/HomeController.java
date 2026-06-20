package com.daily.lastsys.features.home;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import com.daily.lastsys.features.userprogress.UserProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class HomeController {

    private final UserProgressService userProgressService;

    public HomeController(UserProgressService userProgressService) {
        this.userProgressService = userProgressService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            return "home/home";
        }

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("progress", userProgressService.getProgress(loginUser.id()));
        return "home/mainpage";
    }

}
