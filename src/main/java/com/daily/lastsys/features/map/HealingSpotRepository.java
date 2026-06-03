package com.daily.lastsys.features.map;

import com.daily.lastsys.features.emotion.EmotionCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class HealingSpotRepository {

    private static final double EARTH_RADIUS_IN_METERS = 6371000;
    private static final int SEARCH_RADIUS_IN_METERS = 20000;
    private static final int SEARCH_LIMIT = 3;
    private static final int DISTANCE_SCORE_DECAY_IN_METERS = 5000;

    private final JdbcTemplate jdbcTemplate;

    public HealingSpotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HealingSpotCandidate> findNearbyPositiveSpots(BigDecimal latitude, BigDecimal longitude) {
        List<String> positiveLabels = EmotionCatalog.positiveLabels();
        String positiveLabelPlaceholders = positiveLabels.stream()
                .map(label -> "?")
                .collect(Collectors.joining(", "));
        List<Object> parameters = new ArrayList<>();
        parameters.add(DISTANCE_SCORE_DECAY_IN_METERS);
        parameters.add(EARTH_RADIUS_IN_METERS);
        parameters.add(latitude);
        parameters.add(latitude);
        parameters.add(longitude);
        parameters.addAll(positiveLabels);
        parameters.add(SEARCH_RADIUS_IN_METERS);
        parameters.add(SEARCH_LIMIT);

        return jdbcTemplate.query(
                """
                select
                    min(distance_in_meters) as distance_in_meters,
                    max(emotion_label) as emotion_label,
                    location_name,
                    avg(latitude) as latitude,
                    avg(longitude) as longitude,
                    count(*) as positive_count,
                    count(*) / (1 + (min(distance_in_meters) / ?)) as healing_score
                from (
                    select
                        (? * 2 * asin(sqrt(
                            power(sin(radians(latitude - ?) / 2), 2)
                            + cos(radians(?)) * cos(radians(latitude))
                            * power(sin(radians(longitude - ?) / 2), 2)
                        ))) as distance_in_meters,
                        emotion_label,
                        location_name,
                        latitude,
                        longitude
                    from emotion_map_markers
                    where emotion_label in (%s)
                ) nearby_spots
                where distance_in_meters <= ?
                group by location_name
                order by healing_score desc, positive_count desc, distance_in_meters asc
                limit ?
                """.formatted(positiveLabelPlaceholders),
                (resultSet, rowNumber) -> new HealingSpotCandidate(
                        resultSet.getDouble("distance_in_meters"),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("location_name"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getInt("positive_count")
                ),
                parameters.toArray()
        );
    }

    public record HealingSpotCandidate(
            double distanceInMeters,
            String emotionLabel,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude,
            int positiveCount
    ) {
    }
}
