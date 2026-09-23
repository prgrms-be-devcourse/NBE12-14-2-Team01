package com.merge.backend.domain.notification.controller;

import com.merge.backend.domain.notification.dto.NotificationReadResponse;
import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final Rq rq;

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> markAsRead(
        @PathVariable Long notificationId
    ) {
        Long actorUserId =
            rq.getActorId();

        NotificationReadResponse response =
            notificationService.markAsRead(
                actorUserId,
                notificationId
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "알림을 읽음 처리했습니다.",
                response
            )
        );
    }
}
