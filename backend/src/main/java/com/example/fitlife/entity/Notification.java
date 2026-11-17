package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="notifications")
public class Notification {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String userId;
 public String message;
 public LocalDateTime createdAt;
 @Column(name="is_read")
 public boolean read;
}
