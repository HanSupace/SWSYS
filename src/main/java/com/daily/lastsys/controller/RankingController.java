package com.daily.lastsys.controller;

import com.daily.lastsys.dto.LoginUser;
import com.daily.lastsys.dto.RankingPageResponse;
import com.daily.lastsys.service.RankingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/ranking")
    public String ranking(HttpSession session, Model model) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            return "redirect:/";
        }

        RankingPageResponse ranking = rankingService.getRanking(loginUser.id());
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("ranking", ranking);
        return "home/ranking";
    }
}
