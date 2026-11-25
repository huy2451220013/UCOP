package com.ucop.service;

import com.ucop.entity.StockItem;
import com.ucop.entity.Warehouse;

import java.util.List;

public interface StockService {
    List<Warehouse> findAllWarehouses();
    Warehouse saveWarehouse(Long id, String code, String name, String location, String performedBy);
    List<StockItem> findAllStockItems();
    List<StockItem> findLowStockItems();
    StockItem adjustStock(Long warehouseId, Long itemId, int deltaOnHand, int lowStockThreshold, String performedBy);
    int getAvailableStock(Long itemId);
    void reserveStock(Long itemId, int quantity, String performedBy);
    void confirmDeductReservedStock(Long itemId, int quantity, String performedBy);
    void releaseReservedStock(Long itemId, int quantity, String performedBy);
}
