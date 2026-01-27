package com.infragest.infra_notifications_service.repository;

import com.infragest.infra_notifications_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
