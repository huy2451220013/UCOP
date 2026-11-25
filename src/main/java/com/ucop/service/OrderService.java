package com.ucop.service;

import com.ucop.entity.Appointment;
import com.ucop.entity.Order;
import com.ucop.entity.Shipment;
import com.ucop.enums.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrderFromCart(Long accountId, String shippingAddress, String note);
    Optional<Order> findById(Long id);
    List<Order> findOrdersForCustomer(Long accountId);
    List<Order> filterOrdersForStaff(OrderStatus status, String customerKeyword, LocalDate fromDate, LocalDate toDate);
    Order updateOrderStatus(Long orderId, OrderStatus newStatus, String performedBy);
    Order cancelOrder(Long orderId, String reason, String performedBy);

    // 3.7 Shipment & Appointment (1-N: Order -> Shipment / Order -> Appointment)
    Shipment createShipment(Long orderId, String carrier, String trackingNumber, LocalDate estimatedDate, String performedBy);
    Shipment updateShipmentStatus(Long shipmentId, String status, String performedBy);
    List<Shipment> findShipmentsByOrder(Long orderId);
    List<Shipment> findAllShipments();

    Appointment createAppointment(Long orderId, String serviceName, LocalDateTime appointmentTime,
                                  String location, String staffAssigned, String notes, String performedBy);
    Appointment updateAppointmentStatus(Long appointmentId, String status, String performedBy);
    List<Appointment> findAppointmentsByOrder(Long orderId);
    List<Appointment> findAllAppointments();
}
