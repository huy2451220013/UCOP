package com.ucop.repository;

import com.ucop.entity.Order;
import com.ucop.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderCode(String orderCode);

    List<Order> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) " +
           "AND (:customer IS NULL OR LOWER(o.account.username) LIKE LOWER(CONCAT('%', :customer, '%')) " +
           "     OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :customer, '%'))) " +
           "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
           "ORDER BY o.createdAt DESC")
    List<Order> filterOrders(@Param("status") OrderStatus status,
                             @Param("customer") String customer,
                             @Param("fromDate") LocalDateTime fromDate,
                             @Param("toDate") LocalDateTime toDate);

    long countByStatusIn(List<OrderStatus> statuses);
}
