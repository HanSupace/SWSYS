package com.daily.lastsys.features.emotion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmotionSummaryRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmotionSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EmotionSummaryResponse> findRecentSummary(Long userId) {
        return jdbcTemplate.query(
                """
                select emotion_label, count(*) as emotion_count
                from emotion_map_markers
                where user_id = ?
                  and created_at >= current_timestamp - interval '7 days'
                group by emotion_label
                order by emotion_count desc, emotion_label asc
                """,
                (resultSet, rowNumber) -> new EmotionSummaryResponse(
                        resultSet.getString("emotion_label"),
                        resultSet.getInt("emotion_count")
                ),
                userId
        );
    }
}
