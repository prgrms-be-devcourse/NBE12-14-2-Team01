package com.merge.backend.global.security;

import tools.jackson.databind.ObjectMapper;
import com.merge.backend.domain.user.exception.UserErrorCode;
import com.merge.backend.domain.user.service.UserService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.exception.ErrorCode;
import com.merge.backend.global.rq.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final Rq rq;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // context-path(/api/v1)가 포함된 전체 경로 기준 — API 요청이 아니면 인증 체크 자체를 스킵
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 회원가입/로그인/리프레시는 인증 없이 호출되는 Public API라서 필터 검증 대상에서 제외
        if (List.of("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh")
            .contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization: Bearer {accessToken} 형식 — 표준 Bearer 토큰 방식
        String headerAuthorization = rq.getHeader("Authorization", "");

        if (headerAuthorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!headerAuthorization.startsWith("Bearer ")) {
            writeErrorResponse(response, UserErrorCode.INVALID_AUTHORIZATION_HEADER);
            return;
        }

        String accessToken = headerAuthorization.substring("Bearer ".length());
        Map<String, Object> payload = userService.payloadOrNull(accessToken);

        if (payload == null) {
            writeErrorResponse(response, UserErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        Long id = (Long) payload.get("id");
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        UserDetails securityUser = new SecurityUser(email, "", List.of(), id, name);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            securityUser,
            securityUser.getPassword(),
            securityUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    //공통 예외 처리 범위 밖이라서 직접 응답
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode)
        throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
        ));
    }
}