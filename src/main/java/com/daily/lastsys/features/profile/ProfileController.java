package com.daily.lastsys.features.profile;

import com.daily.lastsys.features.dailymission.DailyMissionService;
import com.daily.lastsys.features.dailymission.MissionSettingsForm;
import com.daily.lastsys.features.login.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class ProfileController {

    private final DailyMissionService dailyMissionService;

    public ProfileController(DailyMissionService dailyMissionService) {
        this.dailyMissionService = dailyMissionService;
    }

    @GetMapping("/profile")
    public String showProfile(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            Model model) {
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("missionSettingsForm", new MissionSettingsForm(dailyMissionService.getMissionSettings(loginUser.id())));

        return "home/profile";
    }

    @PostMapping("/profile/mission-settings")
    public String saveMissionSettings(
            @SessionAttribute(name = "loginUser", required = false) LoginUser loginUser,
            @ModelAttribute MissionSettingsForm missionSettingsForm
    ) {
        if (loginUser == null) {
            return "redirect:/login";
        }

        dailyMissionService.saveMissionSettings(loginUser.id(), missionSettingsForm.toSettings());
        return "redirect:/profile?missionSettingsSaved";
    }
}
