package com.daily.lastsys.dto;

public record RankingEntryResponse(
        int rank,
        Long userId,
        String nickname,
        int totalXp,
        UserProgressResponse progress,
        boolean currentUser
) {
}
