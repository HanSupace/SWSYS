package com.daily.lastsys.features.recommendation;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RecommendationController {
    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @PostMapping("/api/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return service.recommend(user.id(), request);
    }
}
