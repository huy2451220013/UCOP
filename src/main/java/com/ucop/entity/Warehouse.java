package com.ucop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // 1-N: Warehouse -> StockItem
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    private List<StockItem> stockItems = new ArrayList<>();

    public Warehouse() {}

    public Warehouse(String code, String name, String location) {
        this.code = code;
        this.name = name;
        this.location = location;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<StockItem> getStockItems() { return stockItems; }
    public void setStockItems(List<StockItem> stockItems) { this.stockItems = stockItems; }
}
