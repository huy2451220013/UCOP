package com.ucop.repository;

import com.ucop.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    List<PromotionUsage> findByPromotionIdOrderByUsedAtDesc(Long promotionId);
    List<PromotionUsage> findTop50ByOrderByUsedAtDesc();
}
