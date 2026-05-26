package com.daily.lastsys.features.emotionrecord;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EmotionRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmotionRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EmotionRecord> findAllByUserId(Long userId) {
        return jdbcTemplate.query(
                """
                select id, user_id, title, content, created_at
                from emotion_records
                where user_id = ?
                order by created_at desc, id desc
                """,
                (resultSet, rowNumber) -> new EmotionRecord(
                        resultSet.getLong("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getTimestamp("created_at").toLocalDateTime()
                ),
                userId
        );
    }

    public Optional<EmotionRecord> findByIdAndUserId(Long id, Long userId) {
        try {
            EmotionRecord record = jdbcTemplate.queryForObject(
                    """
                    select id, user_id, title, content, created_at
                    from emotion_records
                    where id = ? and user_id = ?
                    """,
                    (resultSet, rowNumber) -> new EmotionRecord(
                            resultSet.getLong("id"),
                            resultSet.getLong("user_id"),
                            resultSet.getString("title"),
                            resultSet.getString("content"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ),
                    id,
                    userId
            );
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean deleteByIdAndUserId(Long id, Long userId) {
        int deleted = jdbcTemplate.update(
                "delete from emotion_records where id = ? and user_id = ?",
                id,
                userId
        );
        return deleted > 0;
    }
}
