package com.daily.lastsys.features.map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class HealingSpotRepository {

    private static final double EARTH_RADIUS_IN_METERS = 6371000;
    private static final int SEARCH_RADIUS_IN_METERS = 20000;
    private static final int LIKED_SPOT_LIMIT = 5;

    private final JdbcTemplate jdbcTemplate;

    public HealingSpotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HealingSpotCandidate> findNearbyEmotionSpots(BigDecimal latitude, BigDecimal longitude) {
        return jdbcTemplate.query(
                """
                select
                    marker_id,
                    user_id,
                    distance_in_meters,
                    emotion_label,
                    location_name,
                    latitude,
                    longitude
                from (
                    select
                        id as marker_id,
                        user_id,
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
                ) nearby_spots
                where distance_in_meters <= ?
                order by distance_in_meters asc, marker_id asc
                """,
                (resultSet, rowNumber) -> new HealingSpotCandidate(
                        resultSet.getLong("marker_id"),
                        resultSet.getLong("user_id"),
                        resultSet.getDouble("distance_in_meters"),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("location_name"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class)
                ),
                EARTH_RADIUS_IN_METERS,
                latitude,
                latitude,
                longitude,
                SEARCH_RADIUS_IN_METERS
        );
    }

    public List<LikedSpotCandidate> findMostLikedSpots() {
        return jdbcTemplate.query(
                """
                select
                    m.id,
                    m.emotion_label,
                    m.location_name,
                    m.title,
                    m.description,
                    m.latitude,
                    m.longitude,
                    max(m.created_at) as latest_created_at,
                    count(l.id) as like_count
                from emotion_map_markers m
                left join likes l on l.record_id = m.id
                group by m.id, m.emotion_label, m.location_name, m.title, m.description, m.latitude, m.longitude
                having count(l.id) > 0
                order by like_count desc, latest_created_at desc, m.id desc
                limit ?
                """,
                (resultSet, rowNumber) -> new LikedSpotCandidate(
                        resultSet.getLong("id"),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("location_name"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getInt("like_count")
                ),
                LIKED_SPOT_LIMIT
        );
    }

    public record HealingSpotCandidate(
            Long markerId,
            Long userId,
            double distanceInMeters,
            String emotionLabel,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record LikedSpotCandidate(
            Long id,
            String emotionLabel,
            String locationName,
            String title,
            String description,
            BigDecimal latitude,
            BigDecimal longitude,
            int likeCount
    ) {
    }
}
