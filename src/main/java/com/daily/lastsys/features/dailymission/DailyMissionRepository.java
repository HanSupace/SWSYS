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

    public void increaseSlotRerollCount(Long userId, LocalDate missionDate, int slotIndex) {
        jdbcTemplate.update(
                """
                insert into daily_mission_slot_rerolls (user_id, mission_date, slot_index, reroll_count)
                values (?, ?, ?, 1)
                on duplicate key update
                    reroll_count = reroll_count + 1,
                    updated_at = current_timestamp
                """,
                userId,
                missionDate,
                slotIndex
        );
    }

    public Map<Integer, String> findMissionSlots(Long userId, LocalDate missionDate) {
        List<DailyMissionSlotAssignment> slots = jdbcTemplate.query(
                """
                select slot_index, mission_key
                from daily_mission_slots
                where user_id = ? and mission_date = ?
                """,
                (resultSet, rowNumber) -> new DailyMissionSlotAssignment(
                        resultSet.getInt("slot_index"),
                        resultSet.getString("mission_key")
                ),
                userId,
                missionDate
        );
        Map<Integer, String> assignments = new HashMap<>();

        for (DailyMissionSlotAssignment slot : slots) {
            assignments.put(slot.slotIndex(), slot.missionKey());
        }

        return assignments;
    }

    public void saveMissionSlot(Long userId, LocalDate missionDate, int slotIndex, String missionKey) {
        jdbcTemplate.update(
                """
                insert into daily_mission_slots (user_id, mission_date, slot_index, mission_key)
                values (?, ?, ?, ?)
                on duplicate key update
                    mission_key = values(mission_key),
                    updated_at = current_timestamp
                """,
                userId,
                missionDate,
                slotIndex,
                missionKey
        );
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

    private record SlotRerollCount(int slotIndex, int rerollCount) {
    }

    private record DailyMissionSlotAssignment(int slotIndex, String missionKey) {
    }
}
