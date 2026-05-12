package com.daily.lastsys.controller;

import com.daily.lastsys.dto.LoginUser;
import com.daily.lastsys.service.UserProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class HomeController {

    private static final List<String> QUOTES = List.of(
            "감정은 없애야 할 문제가 아니라 이해해야 할 신호입니다.",
            "오늘의 마음을 알아차리는 것만으로도 충분히 잘하고 있습니다.",
            "흔들리는 마음도 나를 지키려는 방식일 수 있습니다.",
            "감정을 천천히 바라보면 마음은 조금씩 자리를 찾습니다.",
            "좋은 하루는 밝은 감정만 있는 하루가 아니라 내 마음을 돌본 하루입니다.",
            "지금 느끼는 감정은 지나가지만, 돌본 마음은 오래 남습니다."
    );

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
        model.addAttribute("quote", randomQuote());
        model.addAttribute("progress", userProgressService.getProgress(loginUser.id()));
        return "home/mainpage";
    }

    private String randomQuote() {
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }
}
