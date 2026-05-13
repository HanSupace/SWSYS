package com.daily.lastsys.service;

import com.daily.lastsys.dto.DailyMissionListResponse;
import com.daily.lastsys.dto.DailyMissionResponse;
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
}
