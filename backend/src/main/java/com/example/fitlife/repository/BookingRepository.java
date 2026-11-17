package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.Booking;
public interface BookingRepository extends JpaRepository<Booking, String> {
}
