package com.backend.repository;

import com.backend.entity.RuleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RuleTemplateRepository extends JpaRepository<RuleTemplate, Long> {
    Optional<RuleTemplate> findByName(String name);
    boolean existsByName(String name);
}
