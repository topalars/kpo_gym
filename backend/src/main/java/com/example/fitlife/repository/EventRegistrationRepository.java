package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.EventRegistration;
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
}
