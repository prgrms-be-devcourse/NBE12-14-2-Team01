package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.global.rq.Rq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class RegularShiftPatternControllerTest {

    @MockitoBean
    private Rq rq;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private RegularShiftPatternRepository regularShiftPatternRepository;

    private Long workplaceId;
    private Long memberId;
    private WorkplaceMember member;


    @BeforeEach
    void setUp() {

        // 1. 테스트 사용자 생성
        User user = new User();

        ReflectionTestUtils.setField(user, "email", "test@test.com");
        ReflectionTestUtils.setField(user, "passwordHash", "test-password");
        ReflectionTestUtils.setField(user, "name", "테스트 사용자");

        userRepository.save(user);


        // 2. 테스트 근무지 생성
        Workplace workplace = new Workplace();

        ReflectionTestUtils.setField(workplace, "name", "테스트 근무지");
        ReflectionTestUtils.setField(workplace, "inviteCode", "TEST01");

        workplaceRepository.save(workplace);


        // 3. 테스트 근무지 구성원 생성
        member = new WorkplaceMember();

        ReflectionTestUtils.setField(member, "workplace", workplace);
        ReflectionTestUtils.setField(member, "user", user);
        ReflectionTestUtils.setField(member, "role", WorkplaceRole.MANAGER);
        ReflectionTestUtils.setField(member, "joinedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "leftAt", null);

        workplaceMemberRepository.save(member);


        // 4. 실제 생성된 ID 저장
        workplaceId = workplace.getId();
        memberId = member.getId();

        given(rq.getActorId()).willReturn(user.getId());
    }


    // =========================================================
    // PAT-01 정기 근무 등록
    // =========================================================

    @Test
    @DisplayName("정기 근무 등록")
    void t1() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(RegularShiftPatternController.class))
                .andExpect(handler().methodName("register"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("정기 근무가 등록되었습니다."))
                .andExpect(jsonPath("$.data.patternId").exists())
                .andExpect(jsonPath("$.data.memberId").value(memberId))
                .andExpect(jsonPath("$.data.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("14:00:00"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 존재하지 않는 근무지")
    void t2() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/99999/regular-shift-patterns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-001"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 존재하지 않는 근로자")
    void t3() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": 99999,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-002"));
    }


    // =========================================================
    // 시간 경계값
    // =========================================================

    @Test
    @DisplayName("정기 근무 등록 실패 - 시작 시간과 종료 시간이 같다")
    void t4() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "TUESDAY",
                                            "startTime": "09:00",
                                            "endTime": "09:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-006"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 시작 시간이 종료 시간보다 늦다")
    void t5() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "TUESDAY",
                                            "startTime": "15:00",
                                            "endTime": "09:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-006"));
    }


    // =========================================================
    // 요청 데이터 검증
    // =========================================================

    @Test
    @DisplayName("정기 근무 등록 실패 - memberId가 null")
    void t6() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": null,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-001"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 요일이 null")
    void t7() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": null,
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-001"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 시작 시간이 null")
    void t8() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": null,
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-001"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 종료 시간이 null")
    void t9() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": null
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-001"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 잘못된 요일 형식")
    void t10() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "월요일",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-002"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 잘못된 시간 형식")
    void t11() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "25:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-002"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - JSON 형식 오류")
    void t12() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY"
                                            "startTime": "09:00"
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-002"));
    }


    // =========================================================
    // PAT-02 정기 근무 조회
    // =========================================================

    @Test
    @DisplayName("정기 근무 다건 조회")
    void t13() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(RegularShiftPatternController.class))
                .andExpect(handler().methodName("list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }


    @Test
    @DisplayName("정기 근무 조회 실패 - 존재하지 않는 근무지")
    void t14() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/99999/regular-shift-patterns")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-001"));
    }


    // =========================================================
    // PAT-03 정기 근무 수정
    // =========================================================

    @Test
    @DisplayName("정기 근무 수정")
    void t15() throws Exception {

        // 수정할 정기 근무를 먼저 생성
        RegularShiftPattern savedPattern =
                regularShiftPatternRepository.save(
                        new RegularShiftPattern(
                                member,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(14, 0)
                        )
                );

        Long patternId = savedPattern.getId();

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(workplaceId, patternId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "WEDNESDAY",
                                            "startTime": "10:00",
                                            "endTime": "15:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(RegularShiftPatternController.class))
                .andExpect(handler().methodName("update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patternId").value(patternId))
                .andExpect(jsonPath("$.data.memberId").value(memberId))
                .andExpect(jsonPath("$.data.dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.data.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("15:00:00"));

        RegularShiftPattern pattern =
                regularShiftPatternRepository.findById(patternId).orElseThrow();

        assertThat(pattern.getDayOfWeek())
                .isEqualTo(DayOfWeek.WEDNESDAY);

        assertThat(pattern.getStartTime())
                .isEqualTo(LocalTime.of(10, 0));

        assertThat(pattern.getEndTime())
                .isEqualTo(LocalTime.of(15, 0));

        assertThat(pattern.getMember().getId())
                .isEqualTo(memberId);
    }


    @Test
    @DisplayName("정기 근무 수정 실패 - 존재하지 않는 정기 근무")
    void t16() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/99999"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "memberId": %d,
                                            "dayOfWeek": "MONDAY",
                                            "startTime": "09:00",
                                            "endTime": "14:00"
                                        }
                                        """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-003"));
    }


    // =========================================================
    // PAT-04 정기 근무 삭제
    // =========================================================

    @Test
    @DisplayName("정기 근무 삭제")
    void t17() throws Exception {

        // 삭제할 정기 근무를 먼저 생성
        RegularShiftPattern savedPattern =
                regularShiftPatternRepository.save(
                        new RegularShiftPattern(
                                member,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(14, 0)
                        )
                );

        Long patternId = savedPattern.getId();

        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(workplaceId, patternId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(RegularShiftPatternController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        RegularShiftPattern pattern =
                regularShiftPatternRepository.findById(patternId).orElse(null);

        assertThat(pattern).isNull();
    }


    @Test
    @DisplayName("정기 근무 삭제 실패 - 존재하지 않는 정기 근무")
    void t18() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/99999"
                                .formatted(workplaceId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-003"));
    }


    // =========================================================
    // 공통 요청 오류
    // =========================================================

    @Test
    @DisplayName("정기 근무 조회 실패 - workplaceId 타입 오류")
    void t19() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/abc/regular-shift-patterns")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COM-003"));
    }

    // =========================================================
// 추가 테스트 헬퍼
// =========================================================

    /**
     * 테스트용 User 생성
     */
    private User createUser(String email, String name) {

        User user = new User();

        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "passwordHash", "test-password");
        ReflectionTestUtils.setField(user, "name", name);

        return userRepository.save(user);
    }


    /**
     * 테스트용 Workplace 생성
     */
    private Workplace createWorkplace(String name, String inviteCode) {

        Workplace workplace = new Workplace();

        ReflectionTestUtils.setField(workplace, "name", name);
        ReflectionTestUtils.setField(workplace, "inviteCode", inviteCode);

        return workplaceRepository.save(workplace);
    }


    /**
     * 테스트용 WorkplaceMember 생성
     */
    private WorkplaceMember createMember(
            Workplace workplace,
            User user,
            WorkplaceRole role,
            LocalDateTime leftAt
    ) {

        WorkplaceMember workplaceMember = new WorkplaceMember();

        ReflectionTestUtils.setField(
                workplaceMember,
                "workplace",
                workplace
        );

        ReflectionTestUtils.setField(
                workplaceMember,
                "user",
                user
        );

        ReflectionTestUtils.setField(
                workplaceMember,
                "role",
                role
        );

        ReflectionTestUtils.setField(
                workplaceMember,
                "joinedAt",
                LocalDateTime.now()
        );

        ReflectionTestUtils.setField(
                workplaceMember,
                "leftAt",
                leftAt
        );

        return workplaceMemberRepository.save(workplaceMember);
    }


    /**
     * 테스트용 RegularShiftPattern 생성
     */
    private RegularShiftPattern createPattern(
            WorkplaceMember workplaceMember,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {

        return regularShiftPatternRepository.save(
                new RegularShiftPattern(
                        workplaceMember,
                        dayOfWeek,
                        startTime,
                        endTime
                )
        );
    }


// =========================================================
// PAT-01 - 근무자 소속 / 재직 여부
// =========================================================

    @Test
    @DisplayName("정기 근무 등록 실패 - 다른 근무지의 근로자")
    void t20() throws Exception {

        // 다른 근무지
        Workplace otherWorkplace =
                createWorkplace(
                        "다른 근무지",
                        "OTHER01"
                );

        // 다른 근무지에 소속된 사용자
        User otherUser =
                createUser(
                        "other@test.com",
                        "다른 근로자"
                );

        WorkplaceMember otherMember =
                createMember(
                        otherWorkplace,
                        otherUser,
                        WorkplaceRole.EMPLOYEE,
                        null
                );

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(otherMember.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-004"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 퇴사한 근로자")
    void t21() throws Exception {

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User retiredUser =
                createUser(
                        "retired@test.com",
                        "퇴사 근로자"
                );

        WorkplaceMember retiredMember =
                createMember(
                        workplace,
                        retiredUser,
                        WorkplaceRole.EMPLOYEE,
                        LocalDateTime.now()
                );

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(retiredMember.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-005"));
    }


// =========================================================
// PAT-01 - 시간 중복
// 기존 Pattern : MONDAY 09:00 ~ 14:00
// =========================================================

    @Test
    @DisplayName("정기 근무 등록 실패 - 기존 근무와 완전히 같은 시간")
    void t22() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 기존 근무 내부에 포함되는 시간")
    void t23() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 10:00 ~ 12:00

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "10:00",
                                        "endTime": "12:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 기존 근무를 포함하는 시간")
    void t24() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 08:00 ~ 15:00

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "08:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));
    }


    @Test
    @DisplayName("정기 근무 등록 성공 - 기존 근무 시작 경계와 맞닿는 시간")
    void t25() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 08:00 ~ 09:00
        // 겹치지 않음

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "08:00",
                                        "endTime": "09:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }


    @Test
    @DisplayName("정기 근무 등록 성공 - 기존 근무 종료 경계와 맞닿는 시간")
    void t26() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 14:00 ~ 15:00
        // 겹치지 않음

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "14:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 시작 경계를 1분 침범")
    void t27() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 08:00 ~ 09:01

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "08:00",
                                        "endTime": "09:01"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));
    }


    @Test
    @DisplayName("정기 근무 등록 실패 - 종료 경계를 1분 침범")
    void t28() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 기존: 09:00 ~ 14:00
        // 신규: 13:59 ~ 15:00

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "13:59",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));
    }


// =========================================================
// PAT-01 - 같은 시간이어도 허용되는 경우
// =========================================================

    @Test
    @DisplayName("정기 근무 등록 성공 - 시간이 같아도 요일이 다르면 등록 가능")
    void t29() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "TUESDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dayOfWeek").value("TUESDAY"));
    }


    @Test
    @DisplayName("정기 근무 등록 성공 - 같은 요일과 시간이어도 근로자가 다르면 등록 가능")
    void t30() throws Exception {

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User secondUser =
                createUser(
                        "second@test.com",
                        "두번째 근로자"
                );

        WorkplaceMember secondMember =
                createMember(
                        workplace,
                        secondUser,
                        WorkplaceRole.EMPLOYEE,
                        null
                );

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(secondMember.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId")
                        .value(secondMember.getId()));
    }


// =========================================================
// PAT-02 - 조회
// =========================================================

    @Test
    @DisplayName("정기 근무 다건 조회 - 등록된 패턴의 정보가 조회된다")
    void t31() throws Exception {

        RegularShiftPattern monday =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        RegularShiftPattern wednesday =
                createPattern(
                        member,
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(15, 0)
                );

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))

                .andExpect(jsonPath("$.data[0].patternId")
                        .value(monday.getId()))
                .andExpect(jsonPath("$.data[0].memberId")
                        .value(memberId))
                .andExpect(jsonPath("$.data[0].memberName")
                        .value("테스트 사용자"))
                .andExpect(jsonPath("$.data[0].role")
                        .value("MANAGER"))
                .andExpect(jsonPath("$.data[0].dayOfWeek")
                        .value("MONDAY"))
                .andExpect(jsonPath("$.data[0].startTime")
                        .value("09:00:00"))
                .andExpect(jsonPath("$.data[0].endTime")
                        .value("14:00:00"))

                .andExpect(jsonPath("$.data[1].patternId")
                        .value(wednesday.getId()))
                .andExpect(jsonPath("$.data[1].dayOfWeek")
                        .value("WEDNESDAY"))
                .andExpect(jsonPath("$.data[1].startTime")
                        .value("10:00:00"))
                .andExpect(jsonPath("$.data[1].endTime")
                        .value("15:00:00"));
    }


    @Test
    @DisplayName("정기 근무 조회 - 퇴사한 근로자의 패턴은 조회되지 않는다")
    void t32() throws Exception {

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User retiredUser =
                createUser(
                        "retired2@test.com",
                        "퇴사 근로자"
                );

        WorkplaceMember retiredMember =
                createMember(
                        workplace,
                        retiredUser,
                        WorkplaceRole.EMPLOYEE,
                        LocalDateTime.now()
                );

        // 현재 근무 중인 사람
        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // 퇴사한 사람
        createPattern(
                retiredMember,
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].memberId")
                        .value(memberId));
    }


// =========================================================
// PAT-03 - 수정 예외
// =========================================================

    @Test
    @DisplayName("정기 근무 수정 실패 - 다른 근무지의 패턴")
    void t33() throws Exception {

        Workplace otherWorkplace =
                createWorkplace(
                        "다른 근무지",
                        "OTHER02"
                );

        User otherUser =
                createUser(
                        "other2@test.com",
                        "다른 근무지 근로자"
                );

        WorkplaceMember otherMember =
                createMember(
                        otherWorkplace,
                        otherUser,
                        WorkplaceRole.EMPLOYEE,
                        null
                );

        RegularShiftPattern otherPattern =
                createPattern(
                        otherMember,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        // workplaceId는 기본 근무지
        // patternId는 다른 근무지의 Pattern

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        otherPattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "10:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-008"));
    }


    @Test
    @DisplayName("정기 근무 수정 실패 - 변경할 근로자가 다른 근무지 소속")
    void t34() throws Exception {

        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        Workplace otherWorkplace =
                createWorkplace(
                        "다른 근무지",
                        "OTHER03"
                );

        User otherUser =
                createUser(
                        "other3@test.com",
                        "다른 근로자"
                );

        WorkplaceMember otherMember =
                createMember(
                        otherWorkplace,
                        otherUser,
                        WorkplaceRole.EMPLOYEE,
                        null
                );

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "10:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(otherMember.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-004"));
    }


    @Test
    @DisplayName("정기 근무 수정 실패 - 변경할 근로자가 퇴사 상태")
    void t35() throws Exception {

        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User retiredUser =
                createUser(
                        "retired3@test.com",
                        "퇴사 근로자"
                );

        WorkplaceMember retiredMember =
                createMember(
                        workplace,
                        retiredUser,
                        WorkplaceRole.EMPLOYEE,
                        LocalDateTime.now()
                );

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "10:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(retiredMember.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-005"));
    }


    @Test
    @DisplayName("정기 근무 수정 실패 - 다른 패턴과 시간이 겹친다")
    void t36() throws Exception {

        RegularShiftPattern targetPattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0)
                );

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        // target을 13:00 ~ 15:00으로 변경
        // 기존 14:00 ~ 18:00과 겹침

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        targetPattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "13:00",
                                        "endTime": "15:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-007"));

        // 실패했으므로 기존 값이 그대로인지 확인
        RegularShiftPattern pattern =
                regularShiftPatternRepository
                        .findById(targetPattern.getId())
                        .orElseThrow();

        assertThat(pattern.getStartTime())
                .isEqualTo(LocalTime.of(9, 0));

        assertThat(pattern.getEndTime())
                .isEqualTo(LocalTime.of(12, 0));
    }


    @Test
    @DisplayName("정기 근무 수정 성공 - 현재 패턴 자기 자신은 중복 검사에서 제외")
    void t37() throws Exception {

        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        // 자기 자신의 시간과 똑같이 수정
        // 자기 자신을 중복으로 판단하면 안 됨

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "09:00",
                                        "endTime": "14:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patternId")
                        .value(pattern.getId()));
    }


    @Test
    @DisplayName("정기 근무 수정 성공 - 다른 패턴과 경계만 맞닿으면 수정 가능")
    void t38() throws Exception {

        RegularShiftPattern targetPattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(7, 0),
                        LocalTime.of(8, 0)
                );

        createPattern(
                member,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        // target을 08:00 ~ 09:00으로 변경
        // 다음 Pattern이 09:00부터 시작하므로 겹치지 않음

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        targetPattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "memberId": %d,
                                        "dayOfWeek": "MONDAY",
                                        "startTime": "08:00",
                                        "endTime": "09:00"
                                    }
                                    """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        RegularShiftPattern updated =
                regularShiftPatternRepository
                        .findById(targetPattern.getId())
                        .orElseThrow();

        assertThat(updated.getStartTime())
                .isEqualTo(LocalTime.of(8, 0));

        assertThat(updated.getEndTime())
                .isEqualTo(LocalTime.of(9, 0));
    }


// =========================================================
// PAT-04 - 삭제 예외 / 경계
// =========================================================

    @Test
    @DisplayName("정기 근무 삭제 실패 - 다른 근무지의 패턴")
    void t39() throws Exception {

        Workplace otherWorkplace =
                createWorkplace(
                        "다른 근무지",
                        "OTHER04"
                );

        User otherUser =
                createUser(
                        "other4@test.com",
                        "다른 근로자"
                );

        WorkplaceMember otherMember =
                createMember(
                        otherWorkplace,
                        otherUser,
                        WorkplaceRole.EMPLOYEE,
                        null
                );

        RegularShiftPattern otherPattern =
                createPattern(
                        otherMember,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        otherPattern.getId()
                                ))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-008"));

        // 실패했으므로 Pattern은 삭제되지 않아야 함
        assertThat(
                regularShiftPatternRepository
                        .findById(otherPattern.getId())
        ).isPresent();
    }


    @Test
    @DisplayName("정기 근무 삭제 실패 - 이미 삭제한 패턴을 다시 삭제")
    void t40() throws Exception {

        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        Long patternId = pattern.getId();

        // 첫 번째 삭제
        mvc.perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        patternId
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 두 번째 삭제
        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        patternId
                                ))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-003"));
    }


    @Test
    @DisplayName("정기 근무 삭제 성공 - 퇴사한 근로자의 패턴도 삭제 가능")
    void t41() throws Exception {

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User retiredUser =
                createUser(
                        "retired4@test.com",
                        "퇴사 근로자"
                );

        WorkplaceMember retiredMember =
                createMember(
                        workplace,
                        retiredUser,
                        WorkplaceRole.EMPLOYEE,
                        LocalDateTime.now()
                );

        RegularShiftPattern pattern =
                createPattern(
                        retiredMember,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(
                regularShiftPatternRepository
                        .findById(pattern.getId())
        ).isEmpty();
    }

    @Test
    @DisplayName("정기 근무 등록 실패 - EMPLOYEE는 등록할 수 없다")
    void t42() throws Exception {

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User employeeUser =
                createUser(
                        "employee@test.com",
                        "일반 근로자"
                );

        createMember(
                workplace,
                employeeUser,
                WorkplaceRole.EMPLOYEE,
                null
        );

        // 이번 요청을 보내는 로그인 사용자를 EMPLOYEE로 변경
        given(rq.getActorId())
                .willReturn(employeeUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        post("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "memberId": %d,
                                    "dayOfWeek": "MONDAY",
                                    "startTime": "09:00",
                                    "endTime": "14:00"
                                }
                                """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-009"));
    }

    @Test
    @DisplayName("정기 근무 조회 실패 - EMPLOYEE는 조회할 수 없다")
    void t43() throws Exception {

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User employeeUser =
                createUser(
                        "employee43@test.com",
                        "일반 근로자"
                );

        createMember(
                workplace,
                employeeUser,
                WorkplaceRole.EMPLOYEE,
                null
        );

        // 현재 로그인 사용자를 EMPLOYEE로 설정
        given(rq.getActorId())
                .willReturn(employeeUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        get("/workplaces/%d/regular-shift-patterns"
                                .formatted(workplaceId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-009"));
    }


    @Test
    @DisplayName("정기 근무 수정 실패 - EMPLOYEE는 수정할 수 없다")
    void t44() throws Exception {

        // 수정 대상 Pattern은 미리 만들어둔다.
        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User employeeUser =
                createUser(
                        "employee44@test.com",
                        "일반 근로자"
                );

        createMember(
                workplace,
                employeeUser,
                WorkplaceRole.EMPLOYEE,
                null
        );

        // 현재 로그인 사용자를 EMPLOYEE로 설정
        given(rq.getActorId())
                .willReturn(employeeUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        put("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "memberId": %d,
                                    "dayOfWeek": "TUESDAY",
                                    "startTime": "10:00",
                                    "endTime": "15:00"
                                }
                                """.formatted(memberId))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-009"));
    }


    @Test
    @DisplayName("정기 근무 삭제 실패 - EMPLOYEE는 삭제할 수 없다")
    void t45() throws Exception {

        // 삭제 대상 Pattern은 미리 만들어둔다.
        RegularShiftPattern pattern =
                createPattern(
                        member,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                );

        Workplace workplace =
                workplaceRepository.findById(workplaceId)
                        .orElseThrow();

        User employeeUser =
                createUser(
                        "employee45@test.com",
                        "일반 근로자"
                );

        createMember(
                workplace,
                employeeUser,
                WorkplaceRole.EMPLOYEE,
                null
        );

        // 현재 로그인 사용자를 EMPLOYEE로 설정
        given(rq.getActorId())
                .willReturn(employeeUser.getId());

        ResultActions resultActions = mvc
                .perform(
                        delete("/workplaces/%d/regular-shift-patterns/%d"
                                .formatted(
                                        workplaceId,
                                        pattern.getId()
                                ))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SHIFT-009"));

        // 권한이 없어서 실패했으므로 실제 Pattern도 삭제되면 안 됨
        assertThat(
                regularShiftPatternRepository.findById(pattern.getId())
        ).isPresent();
    }
}