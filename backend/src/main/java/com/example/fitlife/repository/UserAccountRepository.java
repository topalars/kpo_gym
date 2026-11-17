package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.UserAccount;
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
 java.util.Optional<UserAccount> findByEmailIgnoreCase(String email);
}
