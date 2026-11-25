package com.ucop.repository;

import com.ucop.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findAllByOrderByCreatedAtDesc();
    List<Refund> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    long countByStatus(String status);
}
