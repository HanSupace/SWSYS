package com.daily.lastsys.service;

import com.daily.lastsys.features.dailymission.DailyMissionDayResponse;
import com.daily.lastsys.features.dailymission.DailyMissionListResponse;
import com.daily.lastsys.features.dailymission.DailyMissionResponse;
import com.daily.lastsys.features.dailymission.DailyMissionService;
import com.daily.lastsys.features.emotion.EmotionCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DailyMissionServiceTests {

    @Autowired
    private DailyMissionService dailyMissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completesTheClickedMissionInsteadOfTheFirstMission() {
        long userId = 91001L;
        jdbcTemplate.update(
                "insert into users (id, username, password_hash, nickname) values (?, ?, ?, ?)",
                userId,
                "mission-user",
                "hash",
                "mission-nickname"
        );

        DailyMissionListResponse beforeCompletion = dailyMissionService.getTodayMissions(userId);
        List<DailyMissionResponse> missions = beforeCompletion.missions();
        DailyMissionResponse clickedMission = missions.get(1);

        DailyMissionListResponse afterCompletion = dailyMissionService.completeMission(userId, clickedMission.id());

        assertThat(afterCompletion.missions())
                .filteredOn(DailyMissionResponse::completed)
                .extracting(DailyMissionResponse::id)
                .containsExactly(clickedMission.id());
        assertThat(afterCompletion.todaySuccessCount()).isEqualTo(1);
    }

    @Test
    void monthlyCalendarEmotionsUseRepresentativeEmotionPerUserAndDate() {
        long firstUserId = 91011L;
        long secondUserId = 91012L;
        insertUser(firstUserId, "calendar-user-a", "calendar-nickname-a");
        insertUser(secondUserId, "calendar-user-b", "calendar-nickname-b");

        insertEmotionMarker(firstUserId, "2026-05-10 09:00:00", "기쁨", emotionColor("기쁨"));
        insertEmotionMarker(firstUserId, "2026-05-10 10:00:00", "슬픔", emotionColor("슬픔"));
        insertEmotionMarker(firstUserId, "2026-05-10 11:00:00", "슬픔", emotionColor("슬픔"));
        insertEmotionMarker(firstUserId, "2026-05-11 09:00:00", "기쁨", emotionColor("기쁨"));
        insertEmotionMarker(firstUserId, "2026-05-11 10:00:00", "기대", emotionColor("기대"));
        insertEmotionMarker(firstUserId, "2026-05-12 09:00:00", "슬픔", "#000000");
        insertEmotionMarker(firstUserId, "2026-05-12 10:00:00", "슬픔", emotionColor("슬픔"));
        insertEmotionMarker(firstUserId, "2026-05-12 11:00:00", "분노", emotionColor("분노"));
        insertEmotionMarker(secondUserId, "2026-05-10 09:00:00", "기대", emotionColor("기대"));

        List<DailyMissionDayResponse> firstUserDays = dailyMissionService.getMonthlyCalendarEmotions(firstUserId, 2026, 5);
        List<DailyMissionDayResponse> secondUserDays = dailyMissionService.getMonthlyCalendarEmotions(secondUserId, 2026, 5);

        assertThat(firstUserDays)
                .extracting(DailyMissionDayResponse::date)
                .doesNotContain(java.time.LocalDate.of(2026, 5, 13));
        assertThat(firstUserDays)
                .filteredOn(day -> day.date().equals(java.time.LocalDate.of(2026, 5, 10)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.emotionLabel()).isEqualTo("슬픔");
                    assertThat(day.emotionColor()).isEqualTo(emotionColor("슬픔"));
                });
        assertThat(firstUserDays)
                .filteredOn(day -> day.date().equals(java.time.LocalDate.of(2026, 5, 11)))
                .singleElement()
                .satisfies(day -> assertThat(day.emotionLabel()).isEqualTo("기대"));
        assertThat(firstUserDays)
                .filteredOn(day -> day.date().equals(java.time.LocalDate.of(2026, 5, 12)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.emotionLabel()).isEqualTo("슬픔");
                    assertThat(day.emotionColor()).isEqualTo(emotionColor("슬픔"));
                });
        assertThat(secondUserDays)
                .filteredOn(day -> day.date().equals(java.time.LocalDate.of(2026, 5, 10)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.emotionLabel()).isEqualTo("기대");
                    assertThat(day.emotionColor()).isEqualTo(emotionColor("기대"));
                });
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

    private void insertEmotionMarker(long userId, String createdAt, String emotionLabel, String emotionColor) {
        jdbcTemplate.update(
                """
                insert into emotion_map_markers (
                    user_id, latitude, longitude, emotion_label, emotion_color,
                    title, location_name, description, created_at, updated_at
                )
                values (?, 37.566826, 126.9786567, ?, ?, 'title', 'location', 'description', ?, ?)
                """,
                userId,
                emotionLabel,
                emotionColor,
                createdAt,
                createdAt
        );
    }

    private String emotionColor(String label) {
        return EmotionCatalog.findByLabel(label)
                .orElseThrow()
                .color();
    }
}
