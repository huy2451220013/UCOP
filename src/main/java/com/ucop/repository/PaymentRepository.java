package com.ucop.repository;

import com.ucop.entity.Payment;
import com.ucop.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM Payment p WHERE p.status IN :statuses")
    BigDecimal sumRevenueByStatuses(@Param("statuses") List<PaymentStatus> statuses);

    @Query("SELECT p.method, COALESCE(SUM(p.grandTotal), 0) FROM Payment p WHERE p.status IN :statuses GROUP BY p.method")
    List<Object[]> sumRevenueByMethod(@Param("statuses") List<PaymentStatus> statuses);
}
