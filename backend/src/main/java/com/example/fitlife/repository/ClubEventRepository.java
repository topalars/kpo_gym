package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.ClubEvent;
public interface ClubEventRepository extends JpaRepository<ClubEvent, String> {
}
