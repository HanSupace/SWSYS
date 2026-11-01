package com.daily.lastsys.features.popularity;

import java.math.BigDecimal;

public record PopularPlaceResponse(
        int rank,
        String placeName,
        String categoryName,
        BigDecimal latitude,
        BigDecimal longitude,
        int searchCount,
        int likeCount,
        int commentCount,
        int popularityScore
) {
}
