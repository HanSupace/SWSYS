package com.daily.lastsys.features.map;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmotionMapMarkerController {

    private final EmotionMapMarkerService emotionMapMarkerService;

    public EmotionMapMarkerController(EmotionMapMarkerService emotionMapMarkerService) {
        this.emotionMapMarkerService = emotionMapMarkerService;
    }

    @GetMapping("/api/emotion-map-markers")
    public List<EmotionMapMarkerResponse> markers(HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.findMarkers(loginUser.id());
    }

    @PostMapping("/api/emotion-map-markers")
    public EmotionMapMarkerResponse createMarker(
            @Valid @RequestBody EmotionMapMarkerRequest request,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.createMarker(loginUser.id(), request);
    }

    @DeleteMapping("/api/emotion-map-markers/{markerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMarker(@PathVariable Long markerId, HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        emotionMapMarkerService.deleteMarker(loginUser.id(), markerId);
    }

    @GetMapping("/api/emotion-map-markers/{markerId}")
    public EmotionMapRecordDetailResponse markerDetail(@PathVariable Long markerId, HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.findRecordDetail(loginUser.id(), markerId);
    }

    @PostMapping("/api/emotion-map-markers/{markerId}/likes")
    public EmotionMapLikeToggleResponse toggleLike(@PathVariable Long markerId, HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.toggleLike(loginUser.id(), markerId);
    }

    @GetMapping("/api/emotion-map-markers/{markerId}/comments")
    public List<EmotionMapCommentResponse> comments(@PathVariable Long markerId, HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.findComments(loginUser.id(), markerId);
    }

    @PostMapping("/api/emotion-map-markers/{markerId}/comments")
    public EmotionMapCommentResponse createComment(
            @PathVariable Long markerId,
            @Valid @RequestBody EmotionMapCommentRequest request,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return emotionMapMarkerService.createComment(loginUser.id(), markerId, request);
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
