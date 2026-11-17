package com.example.fitlife.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
public final class Requests {
 private Requests() {}
 public record Login(@NotBlank @Email String email, @NotBlank String password) {}
 public record Register(@NotBlank @Size(min=2,max=100) String name, @NotBlank @Email @Size(max=150) String email, @NotBlank @Size(min=8,max=72) String password) {}
 public record Profile(@NotBlank @Size(min=2,max=100) String name, @Size(max=30) String phone, @Min(80) @Max(250) Integer height) {}
 public record Trainer(@NotBlank @Size(min=2,max=100) String name, @NotBlank @Email @Size(max=150) String email, @Size(min=8,max=72) String password, @Size(max=30) String phone, @NotBlank @Size(max=150) String specialty, @NotNull Boolean active) {}
 public record Member(@NotBlank @Size(min=2,max=100) String name, @Size(max=30) String phone, @NotNull Boolean active, @NotBlank String plan, LocalDate planUntil) {}
 public record Plan(@NotBlank String plan, LocalDate planUntil) {}
 public record Training(@NotBlank @Size(max=100) String title, String coachId, @NotNull LocalDate date, @NotNull LocalTime time, @NotNull @Min(15) @Max(180) Integer duration, @NotNull @Min(1) @Max(50) Integer capacity, @NotBlank @Size(max=80) String room) {}
 public record Progress(@NotNull @PastOrPresent LocalDate date, @NotNull @DecimalMin("25") @DecimalMax("350") BigDecimal weight, @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal strength) {}
 public record Event(@NotBlank @Size(max=120) String title, @NotBlank @Size(max=1500) String description, @NotNull LocalDate date, @NotNull LocalTime time, @NotBlank @Size(max=50) String type, @NotNull @Min(1) @Max(200) Integer capacity, @NotNull Boolean published) {}
}
