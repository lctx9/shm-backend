package com.backend.repository;

import com.backend.entity.SystemActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemActivityRepository extends JpaRepository<SystemActivity, Long> {
    List<SystemActivity> findTop20ByOrderByCreatedAtDesc();
}
