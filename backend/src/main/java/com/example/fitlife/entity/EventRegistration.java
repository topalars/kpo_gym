package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="event_registrations")
public class EventRegistration {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String eventId;
 public String clientId;
}
