package com.daily.lastsys.dto;

import java.util.List;

public record DailyMissionListResponse(
        List<DailyMissionResponse> missions,
        int todaySuccessCount,
        UserProgressResponse progress
) {
}
