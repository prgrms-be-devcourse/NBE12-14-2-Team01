package com.merge.backend.global.security;

import com.merge.backend.domain.user.exception.UserErrorCode;
import com.merge.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/refresh")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
                    // H2 Console 테스트용
                    .requestMatchers("/h2-console/**").permitAll()
                    // PAT-01 테스트용 임시
                    .requestMatchers("/api/v1/workplaces/**").permitAll()
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                    ).permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(customAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)

            //토큰 없는 요청은 Spring Security가 기본처리 하는데 대신 ApiResponse 형식으로 통일
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> {
                    UserErrorCode errorCode = UserErrorCode.AUTHENTICATION_REQUIRED;
                    response.setStatus(errorCode.getHttpStatus().value());
                    response.setContentType("application/json; charset=UTF-8");
                    String body = objectMapper.writeValueAsString(
                        ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
                    );
                    response.getWriter().write(body);
                })
            );

        return http.build();
    }
}
