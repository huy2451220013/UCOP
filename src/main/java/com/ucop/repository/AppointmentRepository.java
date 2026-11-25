package com.ucop.repository;

import com.ucop.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByOrderIdOrderByAppointmentTimeAsc(Long orderId);
    List<Appointment> findAllByOrderByAppointmentTimeAsc();
}
