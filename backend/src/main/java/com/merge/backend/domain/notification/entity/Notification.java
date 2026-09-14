package com.merge.backend.domain.notification.entity;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id", nullable = false)
    private WorkplaceMember recipientMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    private LocalDateTime readAt;
}