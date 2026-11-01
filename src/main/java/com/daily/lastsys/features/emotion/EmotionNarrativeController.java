package com.daily.lastsys.features.emotion;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class EmotionNarrativeController {
    private final EmotionNarrativeService service;

    public EmotionNarrativeController(EmotionNarrativeService service) {
        this.service = service;
    }

    @GetMapping("/api/emotions/narrative")
    public EmotionNarrativeResponse narrative(HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return service.analyze(user.id());
    }
}
