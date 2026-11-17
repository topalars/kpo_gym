package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.Membership;
public interface MembershipRepository extends JpaRepository<Membership, String> {
}
