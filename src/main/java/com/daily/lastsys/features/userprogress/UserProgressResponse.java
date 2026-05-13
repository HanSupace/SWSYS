package com.daily.lastsys.features.userprogress;

public record UserProgressResponse(
        int level,
        int currentXp,
        int requiredXp,
        int totalXp,
        int progressPercent
) {
}
