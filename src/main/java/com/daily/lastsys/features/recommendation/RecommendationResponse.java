package com.daily.lastsys.features.recommendation;
import com.daily.lastsys.features.map.EmotionMapMarkerResponse;
import java.util.List;
public record RecommendationResponse(RecommendationIntent criteria, List<EmotionMapMarkerResponse> records) {}
