package com.daily.lastsys.features.popularity;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class PopularPlaceController {
    private final PopularPlaceService service;

    public PopularPlaceController(PopularPlaceService service) {
        this.service = service;
    }

    @GetMapping("/api/popular-places")
    public List<PopularPlaceResponse> popularPlaces(HttpSession session) {
        requireLogin(session);
        return service.getPopularPlaces();
    }

    @PostMapping("/api/place-searches")
    public void recordPlaceSearch(@Valid @RequestBody PlaceSearchEventRequest request, HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        service.recordSearch(loginUser.id(), request);
    }

    private LoginUser requireLogin(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(LoginController.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return loginUser;
    }
}
