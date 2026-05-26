package com.daily.lastsys.features.emotionrecord;

import java.time.LocalDateTime;

public record EmotionRecord(
        Long id,
        Long userId,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
