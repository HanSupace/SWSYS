package com.daily.lastsys.features.recommendation;

public record RecommendationSearchDocument(
        Long markerId,
        String emotionLabel,
        String title,
        String locationName,
        String description
) {
}
