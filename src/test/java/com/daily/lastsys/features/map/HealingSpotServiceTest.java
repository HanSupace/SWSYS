package com.daily.lastsys.features.map;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealingSpotServiceTest {

    @Test
    void groupsNearbyPositiveMarkersByCoordinatesInsteadOfLocationName() {
        List<HealingSpotRepository.HealingSpotCandidate> candidates = List.of(
                candidate(1L, 10L, "기쁨", "한강공원", "37.0000", "127.0000"),
                candidate(2L, 11L, "기대", "여의나루 산책로", "37.0008", "127.0000"),
                candidate(3L, 12L, "기쁨", "외딴 장소", "37.0200", "127.0000")
        );
        HealingSpotRepository repository = new HealingSpotRepository(null) {
            @Override
            public List<HealingSpotCandidate> findNearbyEmotionSpots(BigDecimal latitude, BigDecimal longitude) {
                return candidates;
            }
        };

        List<HealingSpotResponse> result = new HealingSpotService(repository)
                .findHealingSpots(new BigDecimal("37.0000"), new BigDecimal("127.0000"));

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().positiveCount());
        assertEquals(37.0004, result.getFirst().lat().doubleValue(), 0.000001);
    }

    @Test
    void excludesClusterWhenPositiveRatioIsBelowSixtyPercent() {
        List<HealingSpotRepository.HealingSpotCandidate> candidates = List.of(
                candidate(1L, 10L, "기쁨", "공원", "37.0000", "127.0000"),
                candidate(2L, 11L, "기대", "산책로", "37.0002", "127.0000"),
                candidate(3L, 12L, "슬픔", "공원 입구", "37.0004", "127.0000"),
                candidate(4L, 13L, "불안", "광장", "37.0006", "127.0000")
        );
        HealingSpotRepository repository = new HealingSpotRepository(null) {
            @Override
            public List<HealingSpotCandidate> findNearbyEmotionSpots(BigDecimal latitude, BigDecimal longitude) {
                return candidates;
            }
        };

        List<HealingSpotResponse> result = new HealingSpotService(repository)
                .findHealingSpots(new BigDecimal("37.0000"), new BigDecimal("127.0000"));

        assertEquals(0, result.size());
    }

    private HealingSpotRepository.HealingSpotCandidate candidate(
            Long markerId,
            Long userId,
            String emotionLabel,
            String locationName,
            String latitude,
            String longitude
    ) {
        return new HealingSpotRepository.HealingSpotCandidate(
                markerId,
                userId,
                0,
                emotionLabel,
                locationName,
                new BigDecimal(latitude),
                new BigDecimal(longitude)
        );
    }
}
