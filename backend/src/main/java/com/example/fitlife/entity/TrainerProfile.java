package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="trainer_profiles")
public class TrainerProfile {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String userId;
 public String specialty;
}
