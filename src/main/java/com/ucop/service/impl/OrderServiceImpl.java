package com.ucop.service.impl;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.*;
import com.ucop.enums.*;
import com.ucop.repository.*;
import com.ucop.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ShipmentRepository shipmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final StockService stockService;
    private final CartService cartService;
    private final AuditService auditService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartRepository cartRepository,
                            PromotionRepository promotionRepository,
                            PromotionUsageRepository promotionUsageRepository,
                            ShipmentRepository shipmentRepository,
                            AppointmentRepository appointmentRepository,
                            StockService stockService,
                            CartService cartService,
                            AuditService auditService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.shipmentRepository = shipmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.stockService = stockService;
        this.cartService = cartService;
        this.auditService = auditService;
    }

    @Override
    public Order placeOrderFromCart(Long accountId, String shippingAddress, String note) {
        Cart cart = cartRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giỏ hàng!"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        String username = cart.getAccount().getUsername();
        PaymentCalculationDTO calc = cartService.calculateCartSummary(cart);

        Order order = new Order();
        order.setOrderCode(generateUniqueOrderCode());
        order.setAccount(cart.getAccount());
        order.setStatus(OrderStatus.PLACED);
        order.setShippingAddress(shippingAddress);
        order.setNote(note);
        order.setCreatedBy(username);
        order.setUpdatedBy(username);

        Promotion promo = (cart.getPromotionCode() != null)
                ? promotionRepository.findByCodeIgnoreCase(cart.getPromotionCode()).orElse(null)
                : null;

        // Reserve stock & create OrderItems inside @Transactional
        for (CartItem ci : cart.getItems()) {
            stockService.reserveStock(ci.getItem().getId(), ci.getQuantity(), username);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setItem(ci.getItem());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());

            BigDecimal lineItemDisc = BigDecimal.ZERO;
            if (promo != null && promo.getScope() == PromotionScope.ITEM
                    && promo.getTargetItem() != null
                    && promo.getTargetItem().getId().equals(ci.getItem().getId())) {
                BigDecimal lineSub = ci.getLineSubtotal();
                if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
                    lineItemDisc = lineSub.multiply(promo.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                } else {
                    lineItemDisc = promo.getDiscountValue().multiply(BigDecimal.valueOf(ci.getQuantity())).min(lineSub);
                }
            }
            oi.setItemDiscount(lineItemDisc);
            order.getOrderItems().add(oi);
        }

        order.setSubtotal(calc.getSubtotal());
        order.setItemDiscount(calc.getItemDiscount());
        order.setCartDiscount(calc.getCartDiscount());
        order.setDiscountAmount(calc.getTotalDiscount());
        order.setTaxAmount(calc.getTaxAmount());
        order.setShippingFee(calc.getShippingFee());
        order.setGrandTotal(calc.getGrandTotal());
        order.setPromotionCode(cart.getPromotionCode());

        Order saved = orderRepository.save(order);

        // Record PromotionUsage (1-N: Promotion -> PromotionUsage)
        if (promo != null && calc.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            promo.setUsedCount(promo.getUsedCount() + 1);
            promotionRepository.save(promo);

            PromotionUsage pu = new PromotionUsage();
            pu.setPromotion(promo);
            pu.setOrder(saved);
            pu.setAccount(cart.getAccount());
            pu.setDiscountAmount(calc.getTotalDiscount());
            promotionUsageRepository.save(pu);
        }

        // Clear Cart
        cart.getItems().clear();
        cart.setPromotionCode(null);
        cart.setItemDiscount(BigDecimal.ZERO);
        cart.setCartDiscount(BigDecimal.ZERO);
        cartRepository.save(cart);

        auditService.log("PLACE_ORDER", "Order", saved.getId(), username,
                "CART", OrderStatus.PLACED.name(),
                "Đặt đơn hàng mới " + saved.getOrderCode() + " - Tổng: " + saved.getGrandTotal().toPlainString() + " đ");
        return saved;
    }

    private String generateUniqueOrderCode() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String candidate = "ORD-" + timePart;
        while (orderRepository.existsByOrderCode(candidate)) {
            int rand = ThreadLocalRandom.current().nextInt(10, 99);
            candidate = "ORD-" + timePart + "-" + rand;
        }
        return candidate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findOrdersForCustomer(Long accountId) {
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> filterOrdersForStaff(OrderStatus status, String customerKeyword, LocalDate fromDate, LocalDate toDate) {
        String cust = (customerKeyword == null || customerKeyword.isBlank()) ? null : customerKeyword.trim();
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        return orderRepository.filterOrders(status, cust, from, to);
    }

    @Override
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedBy(performedBy);
        Order saved = orderRepository.save(order);

        auditService.log("ORDER_STATUS", "Order", saved.getId(), performedBy,
                oldStatus.name(), newStatus.name(),
                "Chuyển trạng thái đơn " + saved.getOrderCode() + ": " + oldStatus + " -> " + newStatus);
        return saved;
    }

    @Override
    public Order cancelOrder(Long orderId, String reason, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CLOSED
                || order.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Đơn hàng đã giao/hoàn tất không thể hủy trực tiếp (vui lòng dùng chức năng Hoàn đổi RMA)!");
        }

        OrderStatus oldStatus = order.getStatus();
        // Release reserved stock if not paid yet
        if (oldStatus == OrderStatus.PLACED || oldStatus == OrderStatus.PENDING_PAYMENT) {
            for (OrderItem oi : order.getOrderItems()) {
                stockService.releaseReservedStock(oi.getItem().getId(), oi.getQuantity(), performedBy);
            }
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setNote((order.getNote() != null && !order.getNote().isBlank() ? order.getNote() + " | " : "") + "HỦY ĐƠN: " + reason);
        order.setUpdatedBy(performedBy);
        Order saved = orderRepository.save(order);

        auditService.log("CANCEL_ORDER", "Order", saved.getId(), performedBy,
                oldStatus.name(), OrderStatus.CANCELED.name(), "Lý do hủy: " + reason);
        return saved;
    }

    @Override
    public Shipment createShipment(Long orderId, String carrier, String trackingNumber, LocalDate estimatedDate, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        Shipment s = new Shipment();
        s.setOrder(order);
        s.setCarrier(carrier);
        s.setTrackingNumber((trackingNumber == null || trackingNumber.isBlank())
                ? "VN" + (System.currentTimeMillis() % 100000000) : trackingNumber.trim().toUpperCase());
        s.setShippingAddress(order.getShippingAddress());
        s.setStatus("IN_TRANSIT");
        s.setShippedAt(LocalDateTime.now());
        if (estimatedDate != null) {
            s.setEstimatedDelivery(estimatedDate.atTime(17, 0));
        }

        Shipment saved = shipmentRepository.save(s);

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PACKED || order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.SHIPPED);
            order.setUpdatedBy(performedBy);
            orderRepository.save(order);
        }

        auditService.log("CREATE_SHIPMENT", "Shipment", saved.getId(), performedBy,
                null, saved.getTrackingNumber(), "Tạo vận đơn cho " + order.getOrderCode());
        return saved;
    }

    @Override
    public Shipment updateShipmentStatus(Long shipmentId, String status, String performedBy) {
        Shipment s = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vận đơn!"));
        String old = s.getStatus();
        s.setStatus(status);
        if ("DELIVERED".equalsIgnoreCase(status)) {
            s.setDeliveredAt(LocalDateTime.now());
            Order o = s.getOrder();
            o.setStatus(OrderStatus.DELIVERED);
            o.setUpdatedBy(performedBy);
            orderRepository.save(o);
        }
        Shipment saved = shipmentRepository.save(s);
        auditService.log("SHIPMENT_STATUS", "Shipment", saved.getId(), performedBy, old, status,
                "Cập nhật vận đơn " + saved.getTrackingNumber());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findShipmentsByOrder(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findAllShipments() {
        return shipmentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Appointment createAppointment(Long orderId, String serviceName, LocalDateTime appointmentTime,
                                         String location, String staffAssigned, String notes, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        Appointment appt = new Appointment();
        appt.setOrder(order);
        appt.setServiceName(serviceName);
        appt.setAppointmentTime(appointmentTime);
        appt.setLocation(location);
        appt.setStaffAssigned(staffAssigned);
        appt.setStatus("CONFIRMED");
        appt.setNotes(notes);
        Appointment saved = appointmentRepository.save(appt);

        auditService.log("CREATE_APPOINTMENT", "Appointment", saved.getId(), performedBy,
                null, "CONFIRMED", "Tạo lịch hẹn dịch vụ cho đơn " + order.getOrderCode());
        return saved;
    }

    @Override
    public Appointment updateAppointmentStatus(Long appointmentId, String status, String performedBy) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn!"));
        String old = appt.getStatus();
        appt.setStatus(status);
        if ("COMPLETED".equalsIgnoreCase(status)) {
            Order o = appt.getOrder();
            o.setStatus(OrderStatus.DELIVERED);
            o.setUpdatedBy(performedBy);
            orderRepository.save(o);
        }
        Appointment saved = appointmentRepository.save(appt);
        auditService.log("APPOINTMENT_STATUS", "Appointment", saved.getId(), performedBy, old, status,
                "Cập nhật lịch hẹn: " + saved.getServiceName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsByOrder(Long orderId) {
        return appointmentRepository.findByOrderIdOrderByAppointmentTimeAsc(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentTimeAsc();
    }
}
