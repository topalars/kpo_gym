package com.example.fitlife.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fitlife.entity.ProgressRecord;
public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, String> {
}
