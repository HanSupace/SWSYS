package com.daily.lastsys.features.login;

import com.daily.lastsys.features.userprogress.UserAccount;
import com.daily.lastsys.features.userprogress.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public void changeNickname(Long userId, String nickname) {
        userRepository.updateNickname(userId, nickname);
    }

    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            return false;
        }

        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
        return true;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        userRepository.deleteById(userId);
    }
}
