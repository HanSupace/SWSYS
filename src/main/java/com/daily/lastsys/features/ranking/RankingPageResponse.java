package com.daily.lastsys.features.ranking;

import java.util.List;

public record RankingPageResponse(
        List<RankingEntryResponse> leaders,
        RankingEntryResponse currentUser,
        boolean currentUserInLeaders
) {
}
