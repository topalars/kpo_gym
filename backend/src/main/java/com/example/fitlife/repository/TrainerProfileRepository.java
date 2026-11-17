package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.TrainerProfile;
public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, String> {
}
