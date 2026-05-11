package com.daily.lastsys.service;

import com.daily.lastsys.dto.SignupForm;
import com.daily.lastsys.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signup(SignupForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new SignupDuplicateException("username", "이미 사용 중인 아이디입니다.");
        }

        if (userRepository.existsByNickname(form.getNickname())) {
            throw new SignupDuplicateException("nickname", "이미 사용 중인 닉네임입니다.");
        }

        String passwordHash = passwordEncoder.encode(form.getPassword());
        userRepository.save(form.getUsername(), passwordHash, form.getNickname());
    }
}
