package com.merge.backend.domain.user.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.exception.UserErrorCode;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.global.exception.BusinessException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public User join(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }
        String passwordHash = passwordEncoder.encode(password);
        User newUser = new User(email, passwordHash, name);
        return userRepository.save(newUser);
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new
                BusinessException(UserErrorCode.AUTHENTICATION_REQUIRED));
    }

    public String genAccessToken(User user) {
        return authTokenService.genAccessToken(user);
    }

    @Transactional
    public String issueRefreshToken(User user) {
        String refreshToken = authTokenService.genRefreshToken(user);
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);
        return refreshToken;
    }

    public String refreshAccessToken(String refreshToken) {
        Long userId = authTokenService.refreshTokenUserIdOrNull(refreshToken);

        if (userId == null) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        return authTokenService.genAccessToken(user);
    }

    public Map<String, Object> payloadOrNull(String jwt) {
        return authTokenService.payloadOrNull(jwt);
    }
}
