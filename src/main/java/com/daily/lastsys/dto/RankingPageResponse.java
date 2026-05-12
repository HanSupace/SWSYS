package com.daily.lastsys.dto;

import java.util.List;

public record RankingPageResponse(
        List<RankingEntryResponse> leaders,
        RankingEntryResponse currentUser,
        boolean currentUserInLeaders
) {
}
