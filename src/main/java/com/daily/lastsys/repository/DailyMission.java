package com.daily.lastsys.repository;

public record DailyMission(
        String missionKey,
        String missionText,
        boolean completed
) {
}
