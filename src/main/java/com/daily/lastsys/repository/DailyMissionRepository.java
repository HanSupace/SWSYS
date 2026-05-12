package com.daily.lastsys.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class DailyMissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DailyMissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int findSuccessCount(Long userId, LocalDate missionDate) {
        Integer successCount = jdbcTemplate.queryForObject(
                """
                select coalesce(max(success_count), 0)
                from daily_mission_days
                where user_id = ? and mission_date = ?
                """,
                Integer.class,
                userId,
                missionDate
        );
        return successCount == null ? 0 : successCount;
    }

    public boolean increaseSuccessCount(Long userId, LocalDate missionDate, int maxSuccessCount) {
        jdbcTemplate.update(
                """
                insert ignore into daily_mission_days (user_id, mission_date, success_count)
                values (?, ?, 0)
                """,
                userId,
                missionDate
        );

        int updated = jdbcTemplate.update(
                """
                update daily_mission_days
                set success_count = success_count + 1,
                    updated_at = current_timestamp
                where user_id = ? and mission_date = ? and success_count < ?
                """,
                userId,
                missionDate,
                maxSuccessCount
        );
        return updated > 0;
    }

    public List<DailyMissionDay> findSuccessCounts(Long userId, LocalDate startDate, LocalDate endDateExclusive) {
        return jdbcTemplate.query(
                """
                select mission_date, success_count
                from daily_mission_days
                where user_id = ?
                  and mission_date >= ?
                  and mission_date < ?
                order by mission_date
                """,
                (resultSet, rowNumber) -> new DailyMissionDay(
                        resultSet.getObject("mission_date", LocalDate.class),
                        resultSet.getInt("success_count")
                ),
                userId,
                startDate,
                endDateExclusive
        );
    }

    public record DailyMissionSeed(String key, String text) {
    }

    public record DailyMissionDay(LocalDate date, int successCount) {
    }
}
