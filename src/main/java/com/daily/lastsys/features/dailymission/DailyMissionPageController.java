package com.daily.lastsys.features.dailymission;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DailyMissionPageController {

    @GetMapping("/dailymission")
    public String showDailyMission(HttpSession session, Model model) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);

        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginUser", loginUser);
        return "home/dailymission";
    }
}
