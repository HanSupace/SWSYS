package com.daily.lastsys.features.map;

import com.daily.lastsys.features.login.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class MapController {
    @GetMapping("/map")
    public String showMap(@SessionAttribute(name = "loginUser", required = false) LoginUser loginUser) {
        if (loginUser == null) {
            return "redirect:/login";
        }

        return "map";
    }
}
