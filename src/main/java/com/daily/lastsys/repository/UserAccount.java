package com.daily.lastsys.repository;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String nickname
) {
}
