package com.daily.lastsys.controller;

import com.daily.lastsys.dto.DailyMissionDayResponse;
import com.daily.lastsys.dto.DailyMissionListResponse;
import com.daily.lastsys.dto.LoginUser;
import com.daily.lastsys.service.DailyMissionService;
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

<<<<<<< Updated upstream:src/main/java/com/daily/lastsys/controller/DailyMissionController.java
=======
    // 🌟 개별 리롤 API (전체 리롤 API는 삭제됨)
    @PostMapping("/api/daily-missions/slots/{slotIndex}/reroll")
    public DailyMissionListResponse rerollMissionSlot(
            @PathVariable int slotIndex,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return dailyMissionService.rerollMissionSlot(loginUser.id(), slotIndex);
    }

>>>>>>> Stashed changes:src/main/java/com/daily/lastsys/features/dailymission/DailyMissionController.java
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