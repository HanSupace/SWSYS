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

    // 🌟 진짜 비밀번호 검증 및 DB 변경 로직
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        UserAccount user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 1. 입력한 현재 비밀번호가 DB의 암호화된 비밀번호와 일치하는지 확인
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            return false;
        }

        // 2. 새 비밀번호 암호화
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        
        // 3. UserRepository를 통해 DB에 업데이트
        userRepository.updatePassword(userId, encodedNewPassword);

        return true;
    }
    // 🌟 추가됨: 닉네임 변경 로직
    public void changeNickname(Long userId, String newNickname) {
        userRepository.updateNickname(userId, newNickname);
    }
}