package com.merge.backend.domain.substitute.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.substitute.entity.RequestCloseReason;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SentSubstituteRequestResponseTest {

    private static final LocalDateTime SHIFT_START = LocalDateTime.of(2026, 9, 25, 9, 0);
    private static final LocalDateTime SHIFT_END = SHIFT_START.plusHours(4);
    private static final Long REQUEST_ID = 77L;
    private static final Long SHIFT_ID = 10L;

    @Test
    @DisplayName("OPEN 상태이고 아직 근무 시작 전이면 그대로 OPEN으로 변환된다")
    void test1() {
        SubstituteRequest request = createRequest(RequestStatus.OPEN, null, null, null);
        LocalDateTime now = SHIFT_START.minusHours(1);

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
        assertThat(response.closeReason()).isNull();
        assertThat(response.closedAt()).isNull();
    }

    @Test
    @DisplayName("OPEN 상태인데 근무 시작 시각이 지났으면 CLOSED/EXPIRED로 변환된다")
    void test2() {
        SubstituteRequest request = createRequest(RequestStatus.OPEN, null, null, null);
        LocalDateTime now = SHIFT_START.plusMinutes(1);

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.status()).isEqualTo(RequestStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(RequestCloseReason.EXPIRED);
        assertThat(response.closedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ACCEPTED 상태에서 now == startAt(경계값)이면 만료로 처리된다")
    void test3() {
        SubstituteRequest request = createRequest(RequestStatus.ACCEPTED, null, null, null);
        LocalDateTime now = SHIFT_START;

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.status()).isEqualTo(RequestStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(RequestCloseReason.EXPIRED);
    }

    @Test
    @DisplayName("APPROVED 상태는 근무 시작 시각이 지나도 그대로 유지된다")
    void test4() {
        LocalDateTime approvedAt = SHIFT_START.minusMinutes(10);
        SubstituteRequest request =
            createRequest(RequestStatus.APPROVED, null, approvedAt, approvedAt);
        LocalDateTime now = SHIFT_START.plusHours(1);

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(response.closeReason()).isNull();
        assertThat(response.approvedAt()).isEqualTo(approvedAt);
        assertThat(response.closedAt()).isEqualTo(approvedAt);
    }

    @Test
    @DisplayName("CLOSED 상태는 저장된 closeReason·closedAt이 그대로 유지된다")
    void test5() {
        LocalDateTime closedAt = SHIFT_START.minusMinutes(5);
        SubstituteRequest request = createRequest(
            RequestStatus.CLOSED, RequestCloseReason.MANAGER_CLOSED, null, closedAt
        );
        LocalDateTime now = SHIFT_START.plusHours(1);

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.status()).isEqualTo(RequestStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(RequestCloseReason.MANAGER_CLOSED);
        assertThat(response.closedAt()).isEqualTo(closedAt);
    }

    @Test
    @DisplayName("requestId·shiftId·workplace 정보가 올바르게 매핑된다")
    void test6() {
        SubstituteRequest request = createRequest(RequestStatus.OPEN, null, null, null);
        LocalDateTime now = SHIFT_START.minusHours(1);

        SentSubstituteRequestResponse response =
            SentSubstituteRequestResponse.from(request, now);

        assertThat(response.requestId()).isEqualTo(REQUEST_ID);
        assertThat(response.shiftId()).isEqualTo(SHIFT_ID);
        assertThat(response.workplaceName()).isEqualTo("SWITCH 카페");
        assertThat(response.startAt()).isEqualTo(SHIFT_START);
        assertThat(response.endAt()).isEqualTo(SHIFT_END);
    }

    private SubstituteRequest createRequest(
        RequestStatus status,
        RequestCloseReason closeReason,
        LocalDateTime approvedAt,
        LocalDateTime closedAt
    ) {
        Workplace workplace = new Workplace("SWITCH 카페", "INVITE01");
        Schedule schedule = new Schedule(workplace, LocalDate.now());
        schedule.publish(LocalDateTime.now());

        WorkplaceMember member = new WorkplaceMember(
            workplace,
            new User(1L, "owner@example.com", "담당자"),
            WorkplaceRole.EMPLOYEE,
            LocalDateTime.now().minusDays(30),
            null
        );
        Shift shift = new Shift(schedule, member, SHIFT_START, SHIFT_END, ShiftStatus.SCHEDULED);
        ReflectionTestUtils.setField(shift, "id", SHIFT_ID);

        SubstituteRequest request = new SubstituteRequest(shift, member, status);
        ReflectionTestUtils.setField(request, "id", REQUEST_ID);
        ReflectionTestUtils.setField(request, "closeReason", closeReason);
        ReflectionTestUtils.setField(request, "approvedAt", approvedAt);
        ReflectionTestUtils.setField(request, "closedAt", closedAt);
        ReflectionTestUtils.setField(request, "createDate", LocalDateTime.now());
        return request;
    }
}
