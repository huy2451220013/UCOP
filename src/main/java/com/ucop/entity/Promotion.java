package com.ucop.entity;

import com.ucop.enums.DiscountType;
import com.ucop.enums.PromotionScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    // CART (Cart-level) hoặc ITEM (Item-level)
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PromotionScope scope = PromotionScope.CART;

    // Nếu scope = ITEM thì áp dụng cho sản phẩm cụ thể này
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_item_id")
    private Item targetItem;

    // PERCENTAGE (%) hoặc FIXED (VND)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @DecimalMin("0.01")
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_usage")
    private Integer maxUsage = 100;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 500)
    private String description;

    // 1-N: Promotion -> PromotionUsage
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL)
    private List<PromotionUsage> usages = new ArrayList<>();

    public boolean isValidNow() {
        if (!active) return false;
        if (maxUsage != null && usedCount >= maxUsage) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public PromotionScope getScope() { return scope; }
    public void setScope(PromotionScope scope) { this.scope = scope; }
    public Item getTargetItem() { return targetItem; }
    public void setTargetItem(Item targetItem) { this.targetItem = targetItem; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public Integer getMaxUsage() { return maxUsage; }
    public void setMaxUsage(Integer maxUsage) { this.maxUsage = maxUsage; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<PromotionUsage> getUsages() { return usages; }
    public void setUsages(List<PromotionUsage> usages) { this.usages = usages; }
}
