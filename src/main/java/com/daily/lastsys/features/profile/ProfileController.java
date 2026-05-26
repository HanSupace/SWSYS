package com.daily.lastsys.features.profile;

import com.daily.lastsys.features.dailymission.DailyMissionService;
import com.daily.lastsys.features.dailymission.MissionSettingsForm;
import com.daily.lastsys.features.emotionrecord.EmotionRecordService;
import com.daily.lastsys.features.login.LoginService;
import com.daily.lastsys.features.login.LoginUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final DailyMissionService dailyMissionService;
    private final LoginService loginService; // 🌟 LoginService 의존성 주입
    private final EmotionRecordService emotionRecordService;

    public ProfileController(
            DailyMissionService dailyMissionService,
            LoginService loginService,
            EmotionRecordService emotionRecordService
    ) {
        this.dailyMissionService = dailyMissionService;
        this.loginService = loginService;
        this.emotionRecordService = emotionRecordService;
    }

    @GetMapping("/profile")
    public String showProfile(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        return "home/profile";
    }

    @GetMapping("/profile/mission-settings")
    public String showMissionSettings(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("missionSettingsForm", new MissionSettingsForm(dailyMissionService.getMissionSettings(loginUser.id())));
        return "home/mission-settings";
    }

    @PostMapping("/profile/mission-settings")
    public String saveMissionSettings(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @ModelAttribute MissionSettingsForm missionSettingsForm
    ) {
        if (loginUser == null) return "redirect:/login";
        dailyMissionService.saveMissionSettings(loginUser.id(), missionSettingsForm.toSettings());
        return "redirect:/profile/mission-settings?missionSettingsSaved=true";
    }

    @GetMapping("/profile/edit")
    public String showEditProfile(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        return "home/profile-edit";
    }
    @PostMapping("/profile/edit")
    public String processEditProfile(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @RequestParam String nickname,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (loginUser == null) return "redirect:/login";

        try {
            // 1. DB의 닉네임 업데이트 (아이디는 건드리지 않음)
            loginService.changeNickname(loginUser.id(), nickname);

            // 2. 화면에 바로 적용되도록 세션 정보 갱신
            LoginUser updatedUser = new LoginUser(loginUser.id(), loginUser.username(), nickname);
            session.setAttribute("loginUser", updatedUser);

            redirectAttributes.addFlashAttribute("successMsg", "프로필 닉네임이 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "프로필 변경 중 오류가 발생했습니다.");
        }

        return "redirect:/profile/edit";
    }

    @GetMapping("/profile/change-password")
    public String showChangePassword(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        return "home/change-password";
    }

    @PostMapping("/profile/change-password")
    public String processChangePassword(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (loginUser == null) return "redirect:/login";

        // 1. 새 비밀번호 일치 검증
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "redirect:/profile/change-password";
        }

        // 2. LoginService를 통해 DB와 통신하여 진짜 비밀번호 검증 실행
        boolean isCurrentPasswordCorrect = loginService.changePassword(loginUser.id(), currentPassword, newPassword);
        
        if (!isCurrentPasswordCorrect) {
            redirectAttributes.addFlashAttribute("errorMsg", "현재 비밀번호가 올바르지 않습니다.");
            return "redirect:/profile/change-password";
        }

        redirectAttributes.addFlashAttribute("successMsg", "비밀번호가 성공적으로 변경되었습니다.");
        return "redirect:/profile/change-password";
    }

    @GetMapping("/profile/emotion-records")
    public String showEmotionRecords(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        addEmotionRecordModel(loginUser, model);
        return "home/emotion-records";
    }

    @GetMapping("/profile/emotion-records/{recordId}")
    public String showEmotionRecordDetail(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @PathVariable Long recordId,
            Model model) {
        if (loginUser == null) return "redirect:/login";
        addEmotionRecordModel(loginUser, model);
        emotionRecordService.getMyRecord(loginUser.id(), recordId)
                .ifPresentOrElse(
                        selectedRecord -> model.addAttribute("selectedRecord", selectedRecord),
                        () -> model.addAttribute("recordError", "감정 기록을 찾을 수 없습니다.")
                );
        return "home/emotion-records";
    }

    @PostMapping("/profile/emotion-records/{recordId}/delete")
    public String deleteEmotionRecord(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @PathVariable Long recordId,
            RedirectAttributes redirectAttributes) {
        if (loginUser == null) return "redirect:/login";

        boolean deleted = emotionRecordService.deleteMyRecord(loginUser.id(), recordId);

        if (deleted) {
            redirectAttributes.addFlashAttribute("successMsg", "감정 기록을 삭제했습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "삭제할 수 없는 감정 기록입니다.");
        }

        return "redirect:/profile/emotion-records";
    }

    @PostMapping("/profile/delete-account")
    public String deleteAccount(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @RequestParam String confirmation,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (loginUser == null) return "redirect:/login";

        if (!"탈퇴".equals(confirmation)) {
            redirectAttributes.addFlashAttribute("deleteErrorMsg", "회원 탈퇴를 진행하려면 '탈퇴'를 정확히 입력해주세요.");
            return "redirect:/profile";
        }

        loginService.deleteAccount(loginUser.id());
        session.invalidate();
        return "redirect:/";
    }

    private void addEmotionRecordModel(LoginUser loginUser, Model model) {
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("emotionRecords", emotionRecordService.getMyRecords(loginUser.id()));
    }
}
