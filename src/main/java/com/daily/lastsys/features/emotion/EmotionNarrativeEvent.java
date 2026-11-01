package com.daily.lastsys.features.emotion;

import java.time.LocalDateTime;

public record EmotionNarrativeEvent(
        String type,
        LocalDateTime createdAt,
        String emotion,
        String title,
        String location,
        String text
) {}
