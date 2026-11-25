package com.ucop.service;

import com.ucop.entity.Promotion;
import com.ucop.entity.PromotionUsage;
import com.ucop.enums.DiscountType;
import com.ucop.enums.PromotionScope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionService {
    List<Promotion> findAll();
    List<Promotion> findActivePromotions();
    Optional<Promotion> findById(Long id);
    Promotion savePromotion(Long id, String code, PromotionScope scope, Long targetItemId,
                            DiscountType discountType, BigDecimal discountValue, BigDecimal minOrderAmount,
                            Integer maxUsage, LocalDate startDate, LocalDate endDate,
                            boolean active, String description, String performedBy);
    void deletePromotion(Long id, String performedBy);
    List<PromotionUsage> findRecentUsages();
}
