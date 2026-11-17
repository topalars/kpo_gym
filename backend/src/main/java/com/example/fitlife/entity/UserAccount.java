package com.example.fitlife.entity;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="users")
public class UserAccount {
 @Id public String id = java.util.UUID.randomUUID().toString();
 public String email;
 public String passwordHash;
 public String name;
 public String role;
 public String phone;
 public boolean active;
}
