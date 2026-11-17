package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="training_sessions")
public class TrainingSession {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String title;
 public String coachId;
 public LocalDate date;
 public LocalTime time;
 public int duration;
 public int capacity;
 public String room;
}
