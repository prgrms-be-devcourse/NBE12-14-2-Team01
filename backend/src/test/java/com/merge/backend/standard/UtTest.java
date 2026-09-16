package com.merge.backend.standard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UtTest {

    private final String secretKey = "test-secret-key-for-jwt-unit-test-1234567890";
    private final long expireMillis = 1000L * 60 * 10;

    @Test
    void JWT를_생성하고_payload를_그대로_복원한다() {
        Map<String, Object> payload = Map.of("id", 1, "email", "test@example.com");

        String jwt = Ut.jwt.toString(secretKey, expireMillis, payload);

        assertThat(jwt).isNotBlank();
        assertThat(Ut.jwt.isValid(jwt, secretKey)).isTrue();
        assertThat(Ut.jwt.payloadOrNull(jwt, secretKey)).containsAllEntriesOf(payload);
    }

    @Test
    void 다른_시크릿키로_검증하면_유효하지_않다() {
        String jwt = Ut.jwt.toString(secretKey, expireMillis, Map.of("id", 1));

        assertThat(Ut.jwt.isValid(jwt, "different-secret-key-1234567890abcdef")).isFalse();
    }

    @Test
    void 만료된_토큰은_유효하지_않다() {
        String jwt = Ut.jwt.toString(secretKey, -1000L, Map.of("id", 1));

        assertThat(Ut.jwt.isValid(jwt, secretKey)).isFalse();
    }
}