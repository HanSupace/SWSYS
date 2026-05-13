package com.daily.lastsys.features.userprogress;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String nickname
) {
}
