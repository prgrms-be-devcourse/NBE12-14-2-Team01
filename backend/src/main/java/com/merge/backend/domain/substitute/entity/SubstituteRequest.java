package com.merge.backend.domain.substitute.entity;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import com.merge.backend.global.exception.BusinessException;
import jakarta.persistence.*;
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

    //3. 요청이 이미 수락, 승인 상태라면 예외 던짐
    public void validateOpening() {
        if (this.status != RequestStatus.OPEN) {
            throw new BusinessException(SubstituteRequestErrorCode.VALIDATE_STATUS_CLOSED);
        }
    }
    //2. 요청이 수락상태로 변하게 하는 메서드
    public void accept() {
        validateOpening();
        //후보자 중 1명이 수락을 누름과 동시에 요청도 수락 상태도 바뀜.
        // ACCEPTED Candidate가 정확히 1명 존재해야 한다는 조건 만족, 추후 동시성 문제 보완 필요)
        this.status = RequestStatus.ACCEPTED;
    }
    public void expiredRequest(){
        this.status = RequestStatus.CLOSED;
        this.closeReason = RequestCloseReason.EXPIRED;
        this.closedAt = LocalDateTime.now();
    }

    //실제로 근무를 바꿈
    public void approveRequest(WorkplaceMember acceptedMember, LocalDateTime now) {
        //Shift 담당자를 대체 근무자로 변경
        this.shift.changeMember(acceptedMember);

        //Request 최종 상태 및 승인/마감 시각 변경
        this.status = RequestStatus.APPROVED;
        this.approvedAt = now;
        this.closedAt = now;
        this.closeReason = null;
    }
}