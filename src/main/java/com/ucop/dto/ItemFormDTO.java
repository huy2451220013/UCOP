package com.ucop.dto;

import com.ucop.enums.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

public class ItemFormDTO {
    private Long id;

    @Pattern(regexp = "^(|[A-Z0-9\\-]{3,30})$", message = "SKU chỉ gồm chữ hoa, số và dấu gạch ngang (3-30 ký tự), hoặc để trống để tự sinh")
    private String sku;

    @NotBlank(message = "Tên sản phẩm / dịch vụ không được để trống")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng chọn loại (Sản phẩm vật lý / Dịch vụ)")
    private ItemType itemType = ItemType.PHYSICAL;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit = "Chiếc";

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "1000", message = "Đơn giá phải từ 1,000 VND trở lên")
    private BigDecimal price;

    @NotNull(message = "Khối lượng không được để trống")
    @Range(min = 0, max = 5000, message = "Khối lượng phải trong khoảng 0 đến 5000 kg")
    private BigDecimal weight = BigDecimal.valueOf(0.5);

    private String status = "ACTIVE";
    private String description;
    private String imageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
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
}
