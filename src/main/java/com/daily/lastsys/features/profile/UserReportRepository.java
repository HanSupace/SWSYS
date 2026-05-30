package com.daily.lastsys.features.profile;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long userId, UserReportForm form) {
        jdbcTemplate.update(
                """
                insert into user_reports (user_id, location_name, title, content, category)
                values (?, ?, ?, ?, ?)
                """,
                userId,
                form.getLocation(),
                form.getTitle(),
                form.getContent(),
                form.getCategory()
        );
    }
}
