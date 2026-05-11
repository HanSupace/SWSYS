package com.daily.lastsys.service;

import com.daily.lastsys.dto.LoginForm;
import com.daily.lastsys.dto.LoginUser;
import com.daily.lastsys.repository.UserAccount;
import com.daily.lastsys.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginUser login(LoginForm form) {
        UserAccount user = userRepository.findByUsername(form.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(form.getPassword(), user.passwordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return new LoginUser(user.id(), user.username(), user.nickname());
    }
}
