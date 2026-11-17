package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="bookings")
public class Booking {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String trainingId;
 public String clientId;
}
