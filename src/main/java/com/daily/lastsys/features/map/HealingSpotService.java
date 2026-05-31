package com.daily.lastsys.features.map;

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
        return switch (emotionLabel) {
            case "기쁨" -> "😊";
            case "평온" -> "🕊️";
            case "기대" -> "🌿";
            case "놀람" -> "✨";
            default -> "🙂";
        };
    }
}
