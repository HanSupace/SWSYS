package com.daily.lastsys.features.map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class EmotionMapMarkerRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmotionMapMarkerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EmotionMapMarkerResponse save(Long userId, EmotionMapMarkerRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into emotion_map_markers (
                        user_id, latitude, longitude, emotion_label, emotion_color,
                        title, location_name, description
                    )
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setBigDecimal(2, request.latitude());
            statement.setBigDecimal(3, request.longitude());
            statement.setString(4, request.emotionLabel());
            statement.setString(5, request.emotionColor());
            statement.setString(6, request.title());
            statement.setString(7, request.locationName());
            statement.setString(8, normalizeDescription(request.description()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        return findById(userId, id);
    }

    public List<EmotionMapMarkerResponse> findAll(Long userId) {
        return jdbcTemplate.query(
                """
                select id, latitude, longitude, emotion_label, emotion_color,
                       title, location_name, description, created_at,
                       case when user_id = ? then true else false end as own
                from emotion_map_markers
                order by created_at desc, id desc
                """,
                (resultSet, rowNumber) -> new EmotionMapMarkerResponse(
                        resultSet.getLong("id"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("emotion_color"),
                        resultSet.getString("title"),
                        resultSet.getString("location_name"),
                        resultSet.getString("description"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getBoolean("own")
                ),
                userId
        );
    }

    public void deleteById(Long userId, Long markerId) {
        jdbcTemplate.update(
                """
                delete from emotion_map_markers
                where user_id = ? and id = ?
                """,
                userId,
                markerId
        );
    }

    private EmotionMapMarkerResponse findById(Long userId, Long markerId) {
        return jdbcTemplate.queryForObject(
                """
                select id, latitude, longitude, emotion_label, emotion_color,
                       title, location_name, description, created_at
                from emotion_map_markers
                where user_id = ? and id = ?
                """,
                (resultSet, rowNumber) -> new EmotionMapMarkerResponse(
                        resultSet.getLong("id"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("emotion_color"),
                        resultSet.getString("title"),
                        resultSet.getString("location_name"),
                        resultSet.getString("description"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        true
                ),
                userId,
                markerId
        );
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description;
    }
}
