package com.daily.lastsys.features.dailymission;

import com.daily.lastsys.features.login.LoginController;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DailyMissionController {

    private final DailyMissionService dailyMissionService;

    public DailyMissionController(DailyMissionService dailyMissionService) {
        this.dailyMissionService = dailyMissionService;
    }

    @GetMapping("/api/daily-missions")
    public DailyMissionListResponse todayMissions(HttpSession session) {
        LoginUser loginUser = requireLogin(session);
        return dailyMissionService.getTodayMissions(loginUser.id());
    }

    @GetMapping("/api/daily-missions/calendar")
    public List<DailyMissionDayResponse> monthlySuccessCounts(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return dailyMissionService.getMonthlySuccessCounts(loginUser.id(), year, month);
    }

    @PostMapping("/api/daily-missions/{missionKey}/complete")
    public DailyMissionListResponse completeMission(
            @PathVariable String missionKey,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return dailyMissionService.completeMission(loginUser.id(), missionKey);
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
