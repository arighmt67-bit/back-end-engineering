package com.arirahmat.helpdesk.notification.repository;

import com.arirahmat.helpdesk.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByEventId(UUID eventId);
}
