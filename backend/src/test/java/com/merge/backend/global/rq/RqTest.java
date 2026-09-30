package com.merge.backend.global.rq;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RqTest {

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    // 쿠키 설정값을 넣어서 Rq 만들기
    private Rq createRq(String domain, boolean secure, String sameSite) {
        Rq rq = new Rq(new MockHttpServletRequest(), response);
        ReflectionTestUtils.setField(rq, "cookieDomain", domain);
        ReflectionTestUtils.setField(rq, "cookieSecure", secure);
        ReflectionTestUtils.setField(rq, "cookieSameSite", sameSite);
        return rq;
    }

    @Test
    @DisplayName("로컬 기본값이면 도메인 없이 Secure와 SameSite도 안 붙인다")
    void test1() {
        Rq rq = createRq("", false, "");

        rq.addCookie("refreshToken", "token");

        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("배포값이면 도메인 없이 Secure와 SameSite를 붙인다")
    void test2() {
        Rq rq = createRq("", true, "Lax");

        rq.addCookie("refreshToken", "token");

        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("도메인을 넣으면 그 도메인으로 쿠키를 만든다")
    void test3() {
        Rq rq = createRq("localhost", false, "");

        rq.addCookie("refreshToken", "token");

        assertThat(response.getCookie("refreshToken").getDomain()).isEqualTo("localhost");
    }

    @Test
    @DisplayName("설정값이 null이어도 쿠키를 만든다")
    void test4() {
        Rq rq = createRq(null, false, null);

        rq.addCookie("refreshToken", "token");

        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getAttribute("SameSite")).isNull();
    }

    @Test
    @DisplayName("쿠키를 지울 때도 같은 속성으로 만든다")
    void test5() {
        Rq rq = createRq("", true, "Lax");

        rq.deleteCookie("refreshToken");

        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
