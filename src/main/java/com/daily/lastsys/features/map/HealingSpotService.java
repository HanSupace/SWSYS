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
}
