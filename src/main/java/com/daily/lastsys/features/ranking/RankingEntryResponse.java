package com.daily.lastsys.features.ranking;

import com.daily.lastsys.features.userprogress.UserProgressResponse;

public record RankingEntryResponse(
        int rank,
        Long userId,
        String nickname,
        int totalXp,
        UserProgressResponse progress,
        boolean currentUser
) {
}
