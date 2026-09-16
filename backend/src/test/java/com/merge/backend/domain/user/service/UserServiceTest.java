package com.merge.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.exception.UserErrorCode;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void 이미_가입된_이메일로_회원가입하면_예외가_발생한다() {
        User existingUser = new User("test@example.com", "hash", "이름");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.join("test@example.com", "password", "이름"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 이메일과_비밀번호가_맞으면_로그인에_성공한다() {
        User user = new User("test@example.com", "encodedPassword", "이름");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        User result = userService.login("test@example.com", "password");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_예외가_발생한다() {
        when(userRepository.findByEmail("no-such@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("no-such@example.com", "password"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 비밀번호가_틀리면_로그인에_실패한다() {
        User user = new User("test@example.com", "encodedPassword", "이름");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("test@example.com", "wrongPassword"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 리프레시_토큰을_발급하면_유저에게_저장된다() {
        User user = new User("test@example.com", "hash", "이름");
        when(authTokenService.genRefreshToken(user)).thenReturn("new-refresh-token");

        String result = userService.issueRefreshToken(user);

        assertThat(result).isEqualTo("new-refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(userRepository).save(user);
    }

    @Test
    void 유효한_리프레시_토큰이면_새_액세스_토큰을_발급한다() {
        User user = new User(1L, "test@example.com", "이름");
        user.updateRefreshToken("refresh-token-value");

        when(authTokenService.refreshTokenUserIdOrNull("refresh-token-value")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authTokenService.genAccessToken(user)).thenReturn("new-access-token");

        String result = userService.refreshAccessToken("refresh-token-value");

        assertThat(result).isEqualTo("new-access-token");
    }

    @Test
    void 리프레시_토큰_서명이_유효하지_않으면_예외가_발생한다() {
        when(authTokenService.refreshTokenUserIdOrNull("invalid")).thenReturn(null);

        assertThatThrownBy(() -> userService.refreshAccessToken("invalid"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 리프레시_토큰의_유저가_존재하지_않으면_예외가_발생한다() {
        when(authTokenService.refreshTokenUserIdOrNull("refresh-token-value")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refreshAccessToken("refresh-token-value"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void DB에_저장된_리프레시_토큰과_다르면_예외가_발생한다() {
        User user = new User(1L, "test@example.com", "이름");
        user.updateRefreshToken("old-refresh-token");

        when(authTokenService.refreshTokenUserIdOrNull("stale-refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.refreshAccessToken("stale-refresh-token"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }
}