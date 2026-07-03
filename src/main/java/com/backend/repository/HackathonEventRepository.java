package com.backend.repository;

import com.backend.entity.HackathonEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, UUID> {
}