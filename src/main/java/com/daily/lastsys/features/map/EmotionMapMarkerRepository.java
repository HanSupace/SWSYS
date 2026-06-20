package com.daily.lastsys.features.map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
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
                    new String[] {"id"}
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
                select m.id, m.latitude, m.longitude, u.nickname as author_nickname,
                       m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description, m.created_at,
                       case when m.user_id = ? then true else false end as own
                from emotion_map_markers m
                join users u on u.id = m.user_id
                order by m.created_at desc, m.id desc
                """,
                (resultSet, rowNumber) -> new EmotionMapMarkerResponse(
                        resultSet.getLong("id"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getString("author_nickname"),
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

    public boolean deleteById(Long userId, Long markerId) {
        int deletedCount = jdbcTemplate.update(
                """
                delete from emotion_map_markers
                where user_id = ? and id = ?
                """,
                userId,
                markerId
        );
        return deletedCount > 0;
    }

    public boolean updateById(Long userId, Long markerId, EmotionMapMarkerRequest request) {
        int updatedCount = jdbcTemplate.update(
                """
                update emotion_map_markers
                set latitude = ?,
                    longitude = ?,
                    emotion_label = ?,
                    emotion_color = ?,
                    title = ?,
                    location_name = ?,
                    description = ?,
                    updated_at = current_timestamp
                where user_id = ? and id = ?
                """,
                request.latitude(),
                request.longitude(),
                request.emotionLabel(),
                request.emotionColor(),
                request.title(),
                request.locationName(),
                normalizeDescription(request.description()),
                userId,
                markerId
        );
        return updatedCount > 0;
    }

    public EmotionMapRecordDetailResponse findDetail(Long userId, Long markerId) {
        return jdbcTemplate.queryForObject(
                """
                select m.id, m.user_id, u.nickname as author_nickname,
                       m.latitude, m.longitude, m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description,
                       m.created_at, m.updated_at,
                       case when m.user_id = ? then true else false end as own,
                       count(distinct l.id) as like_count,
                       count(distinct c.id) as comment_count,
                       case when max(case when ml.user_id is not null then 1 else 0 end) = 1 then true else false end as liked_by_me
                from emotion_map_markers m
                join users u on u.id = m.user_id
                left join likes l on l.record_id = m.id
                left join comments c on c.record_id = m.id
                left join likes ml on ml.record_id = m.id and ml.user_id = ?
                where m.id = ?
                group by m.id, m.user_id, u.nickname, m.latitude, m.longitude, m.emotion_label, m.emotion_color,
                         m.title, m.location_name, m.description, m.created_at, m.updated_at
                """,
                (resultSet, rowNumber) -> new EmotionMapRecordDetailResponse(
                        resultSet.getLong("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("author_nickname"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("emotion_color"),
                        resultSet.getString("title"),
                        resultSet.getString("location_name"),
                        resultSet.getString("description"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject("updated_at", LocalDateTime.class),
                        resultSet.getBoolean("own"),
                        resultSet.getInt("like_count"),
                        resultSet.getInt("comment_count"),
                        resultSet.getBoolean("liked_by_me")
                ),
                userId,
                userId,
                markerId
        );
    }

    public EmotionMapLikeToggleResponse toggleLike(Long userId, Long markerId) {
        try {
            jdbcTemplate.update(
                    """
                    insert into likes (record_id, user_id)
                    values (?, ?)
                    """,
                    markerId,
                    userId
            );
            return new EmotionMapLikeToggleResponse(true, countLikes(markerId));
        } catch (DuplicateKeyException exception) {
            jdbcTemplate.update(
                    """
                    delete from likes
                    where record_id = ? and user_id = ?
                    """,
                    markerId,
                    userId
            );
            return new EmotionMapLikeToggleResponse(false, countLikes(markerId));
        }
    }

    public EmotionMapCommentResponse saveComment(Long userId, Long markerId, EmotionMapCommentRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into comments (record_id, user_id, content)
                    values (?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            statement.setLong(1, markerId);
            statement.setLong(2, userId);
            statement.setString(3, request.content().trim());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        return findCommentById(userId, id);
    }

    public List<EmotionMapCommentResponse> findComments(Long userId, Long markerId) {
        return jdbcTemplate.query(
                """
                select c.id, c.record_id, c.user_id, u.nickname as author_nickname,
                       c.content, c.created_at, c.updated_at,
                       case when c.user_id = ? then true else false end as own
                from comments c
                join users u on u.id = c.user_id
                where c.record_id = ?
                order by c.created_at asc, c.id asc
                """,
                (resultSet, rowNumber) -> mapComment(resultSet),
                userId,
                markerId
        );
    }

    public int countComments(Long markerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from comments where record_id = ?",
                Integer.class,
                markerId
        );
        return count == null ? 0 : count;
    }

    private int countLikes(Long markerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from likes where record_id = ?",
                Integer.class,
                markerId
        );
        return count == null ? 0 : count;
    }

    private EmotionMapMarkerResponse findById(Long userId, Long markerId) {
        return jdbcTemplate.queryForObject(
                """
                select m.id, m.latitude, m.longitude, u.nickname as author_nickname,
                       m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description, m.created_at
                from emotion_map_markers m
                join users u on u.id = m.user_id
                where m.user_id = ? and m.id = ?
                """,
                (resultSet, rowNumber) -> new EmotionMapMarkerResponse(
                        resultSet.getLong("id"),
                        resultSet.getObject("latitude", BigDecimal.class),
                        resultSet.getObject("longitude", BigDecimal.class),
                        resultSet.getString("author_nickname"),
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

    private EmotionMapCommentResponse findCommentById(Long userId, Long commentId) {
        return jdbcTemplate.queryForObject(
                """
                select c.id, c.record_id, c.user_id, u.nickname as author_nickname,
                       c.content, c.created_at, c.updated_at,
                       case when c.user_id = ? then true else false end as own
                from comments c
                join users u on u.id = c.user_id
                where c.id = ?
                """,
                (resultSet, rowNumber) -> mapComment(resultSet),
                userId,
                commentId
        );
    }

    private EmotionMapCommentResponse mapComment(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new EmotionMapCommentResponse(
                resultSet.getLong("id"),
                resultSet.getLong("record_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("author_nickname"),
                resultSet.getString("content"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class),
                resultSet.getBoolean("own")
        );
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description;
    }
}
