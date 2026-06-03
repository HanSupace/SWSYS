package com.daily.lastsys.features.dailymission;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public boolean insertMissionCompletion(Long userId, LocalDate missionDate, String missionKey) {
        int inserted = jdbcTemplate.update(
                """
                insert ignore into daily_mission_completions (user_id, mission_date, mission_key)
                values (?, ?, ?)
                """,
                userId,
                missionDate,
                missionKey
        );
        return inserted > 0;
    }

    public Set<String> findCompletedMissionKeys(Long userId, LocalDate missionDate) {
        return jdbcTemplate.queryForList(
                        """
                        select mission_key
                        from daily_mission_completions
                        where user_id = ? and mission_date = ?
                        """,
                        String.class,
                        userId,
                        missionDate
                ).stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    public MissionSettings findMissionSettings(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    select mission_mode, life_stage, environment_type, condition_type
                    from user_mission_settings
                    where user_id = ?
                    """,
                    (resultSet, rowNumber) -> new MissionSettings(
                            resultSet.getString("mission_mode"),
                            resultSet.getString("life_stage"),
                            resultSet.getString("environment_type"),
                            resultSet.getString("condition_type")
                    ),
                    userId
            );
        } catch (EmptyResultDataAccessException exception) {
            return MissionSettings.defaults();
        }
    }

    public void saveMissionSettings(Long userId, MissionSettings settings) {
        jdbcTemplate.update(
                """
                insert into user_mission_settings (user_id, mission_mode, life_stage, environment_type, condition_type)
                values (?, ?, ?, ?, ?)
                on duplicate key update
                    mission_mode = values(mission_mode),
                    life_stage = values(life_stage),
                    environment_type = values(environment_type),
                    condition_type = values(condition_type),
                    updated_at = current_timestamp
                """,
                userId,
                settings.mode(),
                settings.lifeStage(),
                settings.environmentType(),
                settings.conditionType()
        );
    }

    public int findRerollCount(Long userId, LocalDate missionDate) {
        Integer rerollCount = jdbcTemplate.queryForObject(
                """
                select coalesce(max(reroll_count), 0)
                from daily_mission_rerolls
                where user_id = ? and mission_date = ?
                """,
                Integer.class,
                userId,
                missionDate
        );
        return rerollCount == null ? 0 : rerollCount;
    }

    public void increaseRerollCount(Long userId, LocalDate missionDate) {
        jdbcTemplate.update(
                """
                insert into daily_mission_rerolls (user_id, mission_date, reroll_count)
                values (?, ?, 1)
                on duplicate key update
                    reroll_count = reroll_count + 1,
                    updated_at = current_timestamp
                """,
                userId,
                missionDate
        );
    }

    public Map<Integer, Integer> findSlotRerollCounts(Long userId, LocalDate missionDate) {
        List<SlotRerollCount> rerollCounts = jdbcTemplate.query(
                """
                select slot_index, reroll_count
                from daily_mission_slot_rerolls
                where user_id = ? and mission_date = ?
                """,
                (resultSet, rowNumber) -> new SlotRerollCount(
                        resultSet.getInt("slot_index"),
                        resultSet.getInt("reroll_count")
                ),
                userId,
                missionDate
        );
        Map<Integer, Integer> counts = new HashMap<>();

        for (SlotRerollCount rerollCount : rerollCounts) {
            counts.put(rerollCount.slotIndex(), rerollCount.rerollCount());
        }

        return counts;
    }

    public boolean increaseSlotRerollCount(Long userId, LocalDate missionDate, int slotIndex, int maxRerollCount) {
        int updated = jdbcTemplate.update(
                """
                insert into daily_mission_slot_rerolls (user_id, mission_date, slot_index, reroll_count)
                values (?, ?, ?, 1)
                on duplicate key update
                    reroll_count = if(reroll_count < ?, reroll_count + 1, reroll_count),
                    updated_at = if(reroll_count < ?, current_timestamp, updated_at)
                """,
                userId,
                missionDate,
                slotIndex,
                maxRerollCount,
                maxRerollCount
        );
        return updated > 0 && findSlotRerollCounts(userId, missionDate).getOrDefault(slotIndex, 0) <= maxRerollCount;
    }

    public List<DailyMissionDay> findRepresentativeEmotions(Long userId, LocalDate startDate, LocalDate endDateExclusive) {
        return jdbcTemplate.query(
                """
                select record_date, emotion_label, emotion_color
                from (
                    select
                        date(created_at) as record_date,
                        emotion_label,
                        max(emotion_color) as emotion_color,
                        count(*) as emotion_count,
                        max(created_at) as latest_created_at,
                        row_number() over (
                            partition by date(created_at)
                            order by count(*) desc, max(created_at) desc
                        ) as ranking
                    from emotion_map_markers
                    where user_id = ?
                      and created_at >= ?
                      and created_at < ?
                    group by date(created_at), emotion_label
                ) daily_emotions
                where ranking = 1
                order by record_date
                """,
                (resultSet, rowNumber) -> new DailyMissionDay(
                        resultSet.getObject("record_date", LocalDate.class),
                        resultSet.getString("emotion_label"),
                        resultSet.getString("emotion_color")
                ),
                userId,
                startDate,
                endDateExclusive
        );
    }

    public record DailyMissionSeed(String key, String text) {
    }

    public record DailyMissionDay(LocalDate date, String emotionLabel, String emotionColor) {
    }

    private record SlotRerollCount(int slotIndex, int rerollCount) {
    }
}
