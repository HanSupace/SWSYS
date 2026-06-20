package com.daily.lastsys.features.emotion;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmotionSummaryController {

    private final EmotionSummaryService emotionSummaryService;

    public EmotionSummaryController(EmotionSummaryService emotionSummaryService) {
        this.emotionSummaryService = emotionSummaryService;
    }

    @GetMapping("/api/emotions/summary")
    public List<EmotionSummaryResponse> emotionSummary(HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return emotionSummaryService.findRecentSummary(loginUser.id());
    }

    private LoginUser requireLogin(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);

        if (loginUser == null) {
            throw new LoginRequiredException();
        }

        return loginUser;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    private static class LoginRequiredException extends RuntimeException {
    }
}
