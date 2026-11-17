package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="events")
public class ClubEvent {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String title;
 public String description;
 public LocalDate date;
 public LocalTime time;
 public String type;
 public int capacity;
 public boolean published;
}
