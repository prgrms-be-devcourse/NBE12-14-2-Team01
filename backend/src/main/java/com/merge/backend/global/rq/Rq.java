package com.merge.backend.global.rq;

import com.merge.backend.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${custom.cookie.domain}")
    private String cookieDomain;

    @Value("${custom.cookie.secure}")
    private boolean cookieSecure;

    @Value("${custom.cookie.same-site}")
    private String cookieSameSite;

    public Long getActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return principal.getId();
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
        applyCookieSettings(cookie);

        response.addCookie(cookie);
    }

    public void deleteCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        applyCookieSettings(cookie);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    //만들 때랑 지울 때 같은 속성이어야 브라우저가 지우니까 같이 씀
    private void applyCookieSettings(Cookie cookie) {
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);

        //비어 있으면 도메인 지정 안 함 (응답한 주소 기준으로 저장됨)
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookie.setDomain(cookieDomain);
        }

        //비어 있으면 SameSite 안 붙임
        if (cookieSameSite != null && !cookieSameSite.isBlank()) {
            cookie.setAttribute("SameSite", cookieSameSite);
        }
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
            .ofNullable(request.getHeader(name))
            .filter(headerValue -> !headerValue.isBlank())
            .orElse(defaultValue);
    }
}