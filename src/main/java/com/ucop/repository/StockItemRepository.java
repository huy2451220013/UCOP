package com.ucop.repository;

import com.ucop.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);
    List<StockItem> findByItemIdOrderByOnHandDesc(Long itemId);

    @Query("SELECT s FROM StockItem s WHERE (s.onHand - s.reserved) <= s.lowStockThreshold ORDER BY (s.onHand - s.reserved) ASC")
    List<StockItem> findLowStockItems();

    @Query("SELECT COALESCE(SUM(s.onHand - s.reserved), 0) FROM StockItem s WHERE s.item.id = :itemId")
    int getTotalAvailableByItemId(@Param("itemId") Long itemId);
}
