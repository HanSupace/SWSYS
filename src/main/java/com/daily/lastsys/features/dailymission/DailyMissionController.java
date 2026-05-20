package com.daily.lastsys.features.dailymission;

import com.daily.lastsys.features.login.LoginUser;
import com.daily.lastsys.features.login.LoginController; // 로그인 세션 키를 위해 추가
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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

    // 🌟 개별 리롤 API (충돌 해결 후 복구 완료!)
    @PostMapping("/api/daily-missions/slots/{slotIndex}/reroll")
    public DailyMissionListResponse rerollMissionSlot(
            @PathVariable int slotIndex,
            HttpSession session
    ) {
        LoginUser loginUser = requireLogin(session);
        return dailyMissionService.rerollMissionSlot(loginUser.id(), slotIndex);
    }

    private LoginUser requireLogin(HttpSession session) {
        // 💡 만약 여기서 LOGIN_USER_SESSION_KEY에 빨간 줄이 뜬다면, 
        // 그냥 문자열 "LOGIN_USER" 로 바꿔주세요!
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