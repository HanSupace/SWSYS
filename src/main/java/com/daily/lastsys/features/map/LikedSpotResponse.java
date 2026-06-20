package com.daily.lastsys.features.map;

import java.math.BigDecimal;

public record LikedSpotResponse(
        Long id,
        String name,
        String emotion,
        String emotionLabel,
        int likeCount,
        String description,
        BigDecimal lat,
        BigDecimal lng
) {
}
