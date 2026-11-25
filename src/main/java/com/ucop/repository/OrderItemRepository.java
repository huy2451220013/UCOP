package com.ucop.repository;

import com.ucop.entity.OrderItem;
import com.ucop.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.item.sku, oi.item.name, oi.item.itemType, SUM(oi.quantity), SUM(oi.unitPrice * oi.quantity) " +
           "FROM OrderItem oi WHERE oi.order.status NOT IN :excludedStatuses " +
           "GROUP BY oi.item.sku, oi.item.name, oi.item.itemType " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingItems(@Param("excludedStatuses") List<OrderStatus> excludedStatuses);
}
