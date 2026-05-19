package com.daily.lastsys.features.dailymission;

public record DailyMissionResponse(
        String id,
        String text,
        boolean completed
) {
}
