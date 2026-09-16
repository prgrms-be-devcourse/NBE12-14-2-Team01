package com.merge.backend.domain.user.service;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.standard.Ut;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    @Value("${custom.jwt.secret-key}")
    private String secretPattern;

    @Value("${custom.jwt.expireMills}")
    private long expireMills;

    @Value("${custom.jwt.refresh-expire-mills}")
    private long refreshExpireMills;

    String genAccessToken(User user) {
        return Ut.jwt.toString(
            secretPattern,
            expireMills,
            Map.of("id", user.getId(), "email", user.getEmail(), "name", user.getName())
        );
    }

    String genRefreshToken(User user) {
        return Ut.jwt.toString(
            secretPattern,
            refreshExpireMills,
            Map.of("id", user.getId(), "type", "refresh")
        );
    }

    Map<String, Object> payloadOrNull(String jwt) {
        Map<String, Object> payload = Ut.jwt.payloadOrNull(jwt, secretPattern);

        // refreshToken 등 email/name이 없는 토큰은 accessToken이 아니므로 거부
        if (payload == null || payload.get("email") == null || payload.get("name") == null) {
            return null;
        }

        long id = ((Number) payload.get("id")).longValue();
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        return Map.of("id", id, "email", email, "name", name);
    }

    Long refreshTokenUserIdOrNull(String jwt) {
        Map<String, Object> payload = Ut.jwt.payloadOrNull(jwt, secretPattern);

        if (payload == null || !"refresh".equals(payload.get("type"))) {
            return null;
        }

        return ((Number) payload.get("id")).longValue();
    }
}
