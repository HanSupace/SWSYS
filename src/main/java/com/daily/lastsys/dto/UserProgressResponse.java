package com.daily.lastsys.dto;

public record UserProgressResponse(
        int level,
        int currentXp,
        int requiredXp,
        int totalXp,
        int progressPercent
) {
}
