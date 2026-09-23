package com.merge.backend.domain.notification.repository;

import com.merge.backend.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n
        from Notification n
        join fetch n.recipientMember rm
        join fetch rm.workplace w
        where rm.user.id = :userId
        order by n.createDate desc, n.id desc
        """)
    List<Notification> findAllByUserId(
        @Param("userId") Long userId
    );

}
