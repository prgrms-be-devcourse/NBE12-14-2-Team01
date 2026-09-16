package com.merge.backend.global.rq;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public User getActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser user = (SecurityUser) authentication.getPrincipal();

        return new User(user.getId(), user.getUsername(), user.getName());
    }

    public String getCookieValue(String name, String defaultValue) {
        return Optional
            .ofNullable(request.getCookies())
            .flatMap(
                cookies ->
                    Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals(name))
                        .map(Cookie::getValue)
                        .filter(value -> !value.isBlank())
                        .findFirst()
            )
            .orElse(defaultValue);
    }

    public void addCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        //배포 도메인 확정되면 변경 필요
        cookie.setDomain("localhost");
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
    }

    public void deleteCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setDomain("localhost");
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
            .ofNullable(request.getHeader(name))
            .filter(headerValue -> !headerValue.isBlank())
            .orElse(defaultValue);
    }
}