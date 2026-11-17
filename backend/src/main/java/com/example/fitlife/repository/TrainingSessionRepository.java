package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.TrainingSession;
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, String> {
}
