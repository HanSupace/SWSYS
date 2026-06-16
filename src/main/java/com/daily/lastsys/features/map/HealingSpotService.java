package com.daily.lastsys.features.map;

import com.daily.lastsys.features.emotion.EmotionCatalog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class HealingSpotService {

    private final HealingSpotRepository healingSpotRepository;

    public HealingSpotService(HealingSpotRepository healingSpotRepository) {
        this.healingSpotRepository = healingSpotRepository;
    }

    public List<HealingSpotResponse> findHealingSpots(BigDecimal latitude, BigDecimal longitude) {
        return healingSpotRepository.findNearbyPositiveSpots(latitude, longitude)
                .stream()
                .map(candidate -> new HealingSpotResponse(
                        formatDistance(candidate.distanceInMeters()),
                        emotionEmoji(candidate.emotionLabel()),
                        candidate.locationName(),
                        candidate.latitude(),
                        candidate.longitude(),
                        candidate.positiveCount()
                ))
                .toList();
    }

    public List<LikedSpotResponse> findLikedSpots() {
        return healingSpotRepository.findMostLikedSpots()
                .stream()
                .map(candidate -> new LikedSpotResponse(
                        candidate.id(),
                        displayName(candidate.locationName(), candidate.title()),
                        emotionEmoji(candidate.emotionLabel()),
                        candidate.emotionLabel(),
                        candidate.likeCount(),
                        displayDescription(candidate.description(), candidate.locationName()),
                        candidate.latitude(),
                        candidate.longitude()
                ))
                .toList();
    }

    private String formatDistance(double distanceInMeters) {
        long roundedMeters = Math.round(distanceInMeters);

        if (roundedMeters < 1000) {
            return roundedMeters + "m";
        }

        double kilometers = roundedMeters / 1000.0;
        return String.format(Locale.US, "%.1fkm", kilometers);
    }

    private String emotionEmoji(String emotionLabel) {
        return EmotionCatalog.findByLabel(emotionLabel)
                .map(EmotionCatalog.EmotionMeta::icon)
                .orElse("🙂");
    }

    private String displayName(String locationName, String title) {
        if (locationName != null && !locationName.isBlank()) {
            return locationName.trim();
        }

        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        return "이름 없는 위치";
    }

    private String displayDescription(String description, String locationName) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }

        if (locationName != null && !locationName.isBlank()) {
            return locationName.trim();
        }

        return "";
    }
}
