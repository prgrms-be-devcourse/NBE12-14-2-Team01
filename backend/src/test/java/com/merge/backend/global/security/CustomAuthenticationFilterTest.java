package com.merge.backend.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CustomAuthenticationFilterTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("토큰 없이 요청하면 401과 AUTH-004를 반환한다")
    void test1() throws Exception {
        // 실제 주소처럼 /api/v1을 붙여야 인증 필터가 검사함
        mvc.perform(get("/api/v1/auth/me").contextPath("/api/v1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH-004"));
    }
}
