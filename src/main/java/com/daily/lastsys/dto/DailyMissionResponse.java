package com.daily.lastsys.dto;

public record DailyMissionResponse(
        String id,
        String text,
        boolean completed
) {
}
