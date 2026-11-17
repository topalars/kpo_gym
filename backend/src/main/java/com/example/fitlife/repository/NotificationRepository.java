package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.Notification;
public interface NotificationRepository extends JpaRepository<Notification, String> {
}
