package com.backend.repository;

import com.backend.entity.HackathonEvent;
import com.backend.entity.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Long> {
    List<HackathonEvent> findByIsActiveTrue();
}