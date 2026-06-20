package com.daily.lastsys.features.map;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmotionMapMarkerResponse(
        Long id,
        BigDecimal latitude,
        BigDecimal longitude,
        String authorNickname,
        String emotionLabel,
        String emotionColor,
        String title,
        String locationName,
        String description,
        LocalDateTime createdAt,
        boolean own
) {
}
