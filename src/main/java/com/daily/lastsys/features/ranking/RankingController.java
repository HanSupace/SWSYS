package com.daily.lastsys.features.ranking;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
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
