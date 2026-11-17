package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="client_profiles")
public class ClientProfile {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String userId;
 public Integer height;
}
