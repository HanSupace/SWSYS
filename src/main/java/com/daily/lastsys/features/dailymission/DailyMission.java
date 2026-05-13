package com.daily.lastsys.features.dailymission;

public record DailyMission(
        String missionKey,
        String missionText,
        boolean completed
) {
}
