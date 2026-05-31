package com.daily.lastsys.features.map;

import java.time.LocalDateTime;

public record EmotionMapCommentResponse(
        Long id,
        Long recordId,
        Long userId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean own
) {
}
