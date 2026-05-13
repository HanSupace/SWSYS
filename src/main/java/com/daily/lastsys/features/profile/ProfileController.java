package com.daily.lastsys.features.profile;

import com.daily.lastsys.features.login.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import com.daily.lastsys.features.login.LoginUser;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String showProfile(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {

        model.addAttribute("loginUser", loginUser);

        return "home/profile";
    }
}