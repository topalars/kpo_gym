package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="memberships")
public class Membership {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String userId;
 public String plan;
 public LocalDate planUntil;
}
