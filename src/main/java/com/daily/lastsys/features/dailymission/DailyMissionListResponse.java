package com.daily.lastsys.features.dailymission;

import com.daily.lastsys.features.userprogress.UserProgressResponse;

import java.util.List;

public record DailyMissionListResponse(
        List<DailyMissionResponse> missions,
        int todaySuccessCount,
        UserProgressResponse progress
) {
}
