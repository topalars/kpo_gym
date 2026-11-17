package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="progress_records")
public class ProgressRecord {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String userId;
 public LocalDate date;
 @Column(precision=7, scale=2)
 public BigDecimal weight;
 @Column(precision=7, scale=2)
 public BigDecimal strength;
}
