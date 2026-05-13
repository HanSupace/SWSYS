package com.daily.lastsys.features.userprogress;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserProgressRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProgressRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int findTotalXp(Long userId) {
        try {
            Integer totalXp = jdbcTemplate.queryForObject(
                    "select total_xp from user_progress where user_id = ?",
                    Integer.class,
                    userId
            );
            return totalXp == null ? 0 : totalXp;
        } catch (EmptyResultDataAccessException exception) {
            create(userId);
            return 0;
        }
    }

    public void addXp(Long userId, int xp) {
        createIfMissing(userId);
        jdbcTemplate.update(
                """
                update user_progress
                set total_xp = total_xp + ?,
                    updated_at = current_timestamp
                where user_id = ?
                """,
                xp,
                userId
        );
    }

    public List<UserProgressSnapshot> findLeaderboard() {
        return jdbcTemplate.query(
                """
                select u.id as user_id,
                       u.nickname,
                       coalesce(up.total_xp, 0) as total_xp
                from users u
                left join user_progress up on up.user_id = u.id
                order by coalesce(up.total_xp, 0) desc, u.nickname asc, u.id asc
                """,
                (rs, rowNum) -> new UserProgressSnapshot(
                        rs.getLong("user_id"),
                        rs.getString("nickname"),
                        rs.getInt("total_xp")
                )
        );
    }

    private void createIfMissing(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_progress where user_id = ?",
                Integer.class,
                userId
        );

        if (count == null || count == 0) {
            create(userId);
        }
    }

    private void create(Long userId) {
        jdbcTemplate.update(
                "insert into user_progress (user_id, total_xp) values (?, 0)",
                userId
        );
    }

    public record UserProgressSnapshot(
            Long userId,
            String nickname,
            int totalXp
    ) {
    }
}
