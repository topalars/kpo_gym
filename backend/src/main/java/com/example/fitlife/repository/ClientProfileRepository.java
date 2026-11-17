package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.ClientProfile;
public interface ClientProfileRepository extends JpaRepository<ClientProfile, String> {
}
