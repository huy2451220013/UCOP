package com.ucop.entity;

import com.ucop.enums.ItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType = ItemType.PHYSICAL;

    @Column(name = "unit", length = 30)
    private String unit = "Chiếc";

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight = BigDecimal.valueOf(0.5);

    @Column(name = "status", nullable = false, length = 15)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<StockItem> stockItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDisplayImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        String s = (sku != null ? sku : "").toUpperCase();
        String n = (name != null ? name : "").toLowerCase();
        if (s.contains("DELL") || s.contains("XPS") || n.contains("dell") || n.contains("xps")) {
            return "/images/dell-xps15.jpg";
        }
        if (s.contains("MAC") || n.contains("macbook") || n.contains("apple")) {
            return "/images/macbook-m3.jpg";
        }
        if (s.contains("CAM") || n.contains("camera")) {
            return "/images/iot-cam-ai.jpg";
        }
        if (s.contains("EDU") || n.contains("khóa học") || n.contains("spring") || n.contains("java")) {
            return "/images/edu-spring-pro.jpg";
        }
        if (s.contains("SRV") || s.contains("MAINT") || n.contains("bảo trì") || n.contains("khám")) {
            return "/images/srv-iot-maint.jpg";
        }
        if (s.contains("IOT") || n.contains("cảm biến") || n.contains("iot")) {
            return "/images/iot-kit-pro.jpg";
        }
        return itemType == ItemType.SERVICE ? "/images/edu-spring-pro.jpg" : "/images/dell-xps15.jpg";
    }
    public List<StockItem> getStockItems() { return stockItems; }
    public void setStockItems(List<StockItem> stockItems) { this.stockItems = stockItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
