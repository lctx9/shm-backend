package com.backend.repository;

import com.backend.entity.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, UUID> {

    // 1. Lấy tất cả giải thưởng của một đội
    List<Prize> findByTeamId(UUID teamId);

    // 2. Lấy tất cả giải thưởng ĐÃ CÔNG BỐ của một đội (để check có được xuất bằng khen không)
    List<Prize> findByTeamIdAndIsAnnouncedTrue(UUID teamId);

    // 3. Lấy 1 giải thưởng cụ thể của 1 đội (để xuất PDF riêng cho từng giải)
    Optional<Prize> findByTeamIdAndId(UUID teamId, UUID prizeId);
}