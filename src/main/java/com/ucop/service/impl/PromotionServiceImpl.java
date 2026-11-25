package com.ucop.service.impl;

import com.ucop.entity.Item;
import com.ucop.entity.Promotion;
import com.ucop.entity.PromotionUsage;
import com.ucop.enums.DiscountType;
import com.ucop.enums.PromotionScope;
import com.ucop.repository.ItemRepository;
import com.ucop.repository.PromotionRepository;
import com.ucop.repository.PromotionUsageRepository;
import com.ucop.service.AuditService;
import com.ucop.service.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ItemRepository itemRepository;
    private final AuditService auditService;

    public PromotionServiceImpl(PromotionRepository promotionRepository,
                                PromotionUsageRepository promotionUsageRepository,
                                ItemRepository itemRepository,
                                AuditService auditService) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return promotionRepository.findAllByOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findActivePromotions() {
        return promotionRepository.findAllByOrderByIdDesc().stream()
                .filter(Promotion::isValidNow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    @Override
    public Promotion savePromotion(Long id, String code, PromotionScope scope, Long targetItemId,
                                   DiscountType discountType, BigDecimal discountValue, BigDecimal minOrderAmount,
                                   Integer maxUsage, LocalDate startDate, LocalDate endDate,
                                   boolean active, String description, String performedBy) {
        boolean isNew = (id == null || id <= 0);
        Promotion p = isNew ? new Promotion()
                : promotionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi!"));

        String cleanCode = code.trim().toUpperCase();
        if (isNew && promotionRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new IllegalArgumentException("Mã khuyến mãi '" + cleanCode + "' đã tồn tại!");
        }

        p.setCode(cleanCode);
        p.setScope(scope);
        if (scope == PromotionScope.ITEM && targetItemId != null && targetItemId > 0) {
            Item item = itemRepository.findById(targetItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm áp dụng!"));
            p.setTargetItem(item);
        } else {
            p.setTargetItem(null);
        }

        p.setDiscountType(discountType);
        p.setDiscountValue(discountValue);
        p.setMinOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO);
        p.setMaxUsage(maxUsage);
        p.setStartDate(startDate != null ? startDate.atStartOfDay() : LocalDate.now().atStartOfDay());
        p.setEndDate(endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().plusMonths(3).atTime(LocalTime.MAX));
        p.setActive(active);
        p.setDescription(description);

        Promotion saved = promotionRepository.save(p);
        auditService.log(isNew ? "CREATE_PROMOTION" : "UPDATE_PROMOTION",
                "Promotion", saved.getId(), performedBy, null, saved.getCode(),
                "Lưu khuyến mãi " + saved.getCode() + " (" + scope + " - " + discountType + ")");
        return saved;
    }

    @Override
    public void deletePromotion(Long id, String performedBy) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã khuyến mãi!"));
        try {
            promotionRepository.delete(p);
            promotionRepository.flush();
            auditService.log("DELETE_PROMOTION", "Promotion", id, performedBy, p.getCode(), null, "Xóa mã khuyến mãi");
        } catch (Exception e) {
            p.setActive(false);
            promotionRepository.save(p);
            auditService.log("DEACTIVATE_PROMOTION", "Promotion", id, performedBy, "ACTIVE", "INACTIVE",
                    "Ngừng kích hoạt mã khuyến mãi đã phát sinh giao dịch: " + p.getCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionUsage> findRecentUsages() {
        return promotionUsageRepository.findTop50ByOrderByUsedAtDesc();
    }
}
