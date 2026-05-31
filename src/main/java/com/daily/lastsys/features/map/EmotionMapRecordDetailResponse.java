package com.daily.lastsys.features.map;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmotionMapRecordDetailResponse(
        Long id,
        Long userId,
        String authorNickname,
        BigDecimal latitude,
        BigDecimal longitude,
        String emotionLabel,
        String emotionColor,
        String title,
        String locationName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean own,
        int likeCount,
        int commentCount,
        boolean likedByMe
) {
}
