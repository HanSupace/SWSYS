package com.daily.lastsys.features.userprogress;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    public boolean existsByNickname(String nickname) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where nickname = ?",
                Integer.class,
                nickname
        );
        return count != null && count > 0;
    }

    public Optional<UserAccount> findByUsername(String username) {
        try {
            UserAccount user = jdbcTemplate.queryForObject(
                    "select id, username, password_hash, nickname from users where username = ?",
                    (rs, rowNum) -> new UserAccount(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("nickname")
                    ),
                    username
            );
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void save(String username, String passwordHash, String nickname) {
        try {
            jdbcTemplate.update(
                    "insert into users (username, password_hash, nickname) values (?, ?, ?)",
                    username,
                    passwordHash,
                    nickname
            );
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("이미 사용 중인 아이디 또는 닉네임입니다.", exception);
        }
    }
}
