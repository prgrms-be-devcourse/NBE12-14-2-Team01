package com.merge.backend.domain.substitute.entity;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import com.merge.backend.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SubstituteRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_member_id", nullable = false)
    private WorkplaceMember requesterMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private RequestCloseReason closeReason;

    private LocalDateTime approvedAt;

    private LocalDateTime closedAt;

    public SubstituteRequest(
        Shift shift,
        WorkplaceMember requesterMember,
        RequestStatus status
    ) {
        this.shift = shift;
        this.requesterMember = requesterMember;
        this.status = status;
    }

    // Request가 아직 OPEN 상태인지 확인
    public void validateOpening() {
        if (this.status != RequestStatus.OPEN) {
            throw new BusinessException(
                SubstituteRequestErrorCode.VALIDATE_STATUS_CLOSED
            );
        }
    }

    // Candidate가 대타 요청을 수락하면 Request도 ACCEPTED로 변경
    public void accept() {
        validateOpening();

        this.status = RequestStatus.ACCEPTED;
    }

    // 모든 Candidate가 거절한 경우 Request 종료
    public void closeAllCandidatesRejected(LocalDateTime now) {
        this.status = RequestStatus.CLOSED;
        this.closeReason = RequestCloseReason.ALL_CANDIDATES_REJECTED;
        this.closedAt = now;
    }

    // MANAGER가 최종 승인하면 실제 Shift 담당자 변경
    public void approveRequest(
        WorkplaceMember acceptedMember,
        LocalDateTime now
    ) {
        // Shift 담당자를 대타 근무자로 변경
        this.shift.changeMember(acceptedMember);

        // Request 최종 상태 및 승인/마감 시각 변경
        this.status = RequestStatus.APPROVED;
        this.approvedAt = now;
        this.closedAt = now;
        this.closeReason = null;
    }

    //요청 종료
    public void closeByManager(LocalDateTime now) {
        this.status = RequestStatus.CLOSED;
        this.closeReason = RequestCloseReason.MANAGER_CLOSED;
        this.closedAt = now;
        this.approvedAt = null;
    }
}