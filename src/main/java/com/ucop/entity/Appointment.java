package com.ucop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1-N: Order -> Appointment (Dành cho Gói dịch vụ / Khám bệnh / Khóa học / Bảo trì IoT)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "staff_assigned", length = 100)
    private String staffAssigned;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SCHEDULED"; // SCHEDULED, CONFIRMED, COMPLETED, CANCELED

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStaffAssigned() { return staffAssigned; }
    public void setStaffAssigned(String staffAssigned) { this.staffAssigned = staffAssigned; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
