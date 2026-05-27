package com.daily.lastsys.service;

import com.daily.lastsys.features.emotionrecord.EmotionRecordResponse;
import com.daily.lastsys.features.emotionrecord.EmotionRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EmotionRecordServiceTests {

    @Autowired
    private EmotionRecordService emotionRecordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsAndDeletesOnlyTheLoginUsersRecords() {
        long ownerId = 92001L;
        long otherUserId = 92002L;
        long ownerRecordId = 920101L;
        long otherRecordId = 920201L;

        insertUser(ownerId, "emotion-owner", "owner-nick");
        insertUser(otherUserId, "emotion-other", "other-nick");
        insertRecord(ownerRecordId, ownerId, "내 기록", "내가 작성한 전체 내용입니다.", LocalDateTime.of(2026, 5, 27, 9, 30));
        insertRecord(otherRecordId, otherUserId, "다른 사람 기록", "다른 사용자가 작성한 내용입니다.", LocalDateTime.of(2026, 5, 27, 10, 30));

        List<EmotionRecordResponse> records = emotionRecordService.getMyRecords(ownerId);

        assertThat(records)
                .extracting(EmotionRecordResponse::id)
                .containsExactly(ownerRecordId);
        assertThat(emotionRecordService.getMyRecord(ownerId, otherRecordId)).isEmpty();
        assertThat(emotionRecordService.deleteMyRecord(ownerId, otherRecordId)).isFalse();
        assertThat(emotionRecordService.getMyRecord(otherUserId, otherRecordId)).isPresent();

        assertThat(emotionRecordService.deleteMyRecord(ownerId, ownerRecordId)).isTrue();
        assertThat(emotionRecordService.getMyRecords(ownerId)).isEmpty();
    }

    private void insertUser(long userId, String username, String nickname) {
        jdbcTemplate.update(
                "insert into users (id, username, password_hash, nickname) values (?, ?, ?, ?)",
                userId,
                username,
                "hash",
                nickname
        );
    }

    private void insertRecord(long recordId, long userId, String title, String content, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                insert into emotion_map_markers (
                    id, user_id, latitude, longitude, emotion_label, emotion_color,
                    title, location_name, description, created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                recordId,
                userId,
                37.566826,
                126.978656,
                "기쁨",
                "#F4B942",
                title,
                "서울",
                content,
                Timestamp.valueOf(createdAt)
        );
    }
}
