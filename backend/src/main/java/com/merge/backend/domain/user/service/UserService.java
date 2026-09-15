package com.merge.backend.domain.user.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User join(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 사용중인 이메일입니다");
        }
        String passwordHash = passwordEncoder.encode(password);
        User newUser = new User(email, passwordHash, name);
        return userRepository.save(newUser);
    }
}
