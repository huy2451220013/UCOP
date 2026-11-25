package com.ucop.repository;

import com.ucop.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<Shipment> findAllByOrderByCreatedAtDesc();
}
