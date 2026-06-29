package com.backend.repository;

import com.backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByEmailOrderByIdDesc(String email);

    Optional<VerificationCode> findFirstByEmailOrderByExpiryTimeDesc(String email);
}