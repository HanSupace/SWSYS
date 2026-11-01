package com.daily.lastsys.features.emotion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class EmotionNarrativeRepository {
    private final JdbcTemplate jdbc;

    public EmotionNarrativeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EmotionNarrativeEvent> recentEvents(long userId) {
        return jdbc.query("""
                select type, created_at, emotion, title, location, text
                from (
                    select 'record' as type, created_at, emotion_label as emotion,
                           title, location_name as location, description as text
                    from (
                        select created_at, emotion_label, title, location_name, description
                        from emotion_map_markers
                        where user_id = ?
                        order by created_at desc, id desc
                        limit 40
                    ) recent_records
                    union all
                    select 'comment' as type, c.created_at, null as emotion,
                           null as title, m.location_name as location, c.content as text
                    from (
                        select record_id, created_at, content
                        from comments
                        where user_id = ?
                        order by created_at desc, id desc
                        limit 20
                    ) c
                    join emotion_map_markers m on m.id = c.record_id
                ) events
                order by created_at desc
                limit 60
                """, (rs, row) -> new EmotionNarrativeEvent(
                rs.getString("type"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getString("emotion"),
                rs.getString("title"),
                rs.getString("location"),
                rs.getString("text")
        ), userId, userId);
    }
}
