package com.ucop.controller;

import com.ucop.entity.Order;
import com.ucop.enums.ItemType;
import com.ucop.enums.OrderStatus;
import com.ucop.service.OrderService;
import com.ucop.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(value = "status", required = false) OrderStatus status,
                             @RequestParam(value = "customer", required = false) String customer,
                             @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                             @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                             HttpSession session,
                             Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        List<Order> orders;
        if (isAdmin || isStaff) {
            orders = orderService.filterOrdersForStaff(status, customer, fromDate, toDate);
        } else {
            orders = orderService.findOrdersForCustomer(accountId).stream()
                    .filter(o -> status == null || o.getStatus() == status)
                    .filter(o -> fromDate == null || (o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isBefore(fromDate)))
                    .filter(o -> toDate == null || (o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isAfter(toDate)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("orders", orders);
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("customer", customer);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "order/list";
    }

    @PostMapping("/orders/checkout")
    public String placeOrder(@RequestParam("shippingAddress") String shippingAddress,
                             @RequestParam(value = "note", required = false) String note,
                             HttpSession session,
                             RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            Order order = orderService.placeOrderFromCart(accountId, shippingAddress, note);
            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + order.getOrderCode() + ". Vui lòng chọn phương thức thanh toán.");
            return "redirect:/payment/checkout/" + order.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/detail/{id}")
    public String orderDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        if (!isAdmin && !isStaff && !order.getAccount().getId().equals(accountId)) {
            return "redirect:/access-denied";
        }

        boolean hasPhysical = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getItem() != null && oi.getItem().getItemType() == ItemType.PHYSICAL);
        boolean hasService = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getItem() != null && oi.getItem().getItemType() == ItemType.SERVICE);

        model.addAttribute("order", order);
        model.addAttribute("payment", paymentService.findByOrderId(id).orElse(null));
        model.addAttribute("refunds", paymentService.findRefundsByOrder(id));
        model.addAttribute("shipments", orderService.findShipmentsByOrder(id));
        model.addAttribute("appointments", orderService.findAppointmentsByOrder(id));
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("hasPhysical", hasPhysical);
        model.addAttribute("hasService", hasService);
        return "order/detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OrderStatus status,
                               HttpSession session,
                               RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) {
            return "redirect:/access-denied";
        }
        try {
            String user = (String) session.getAttribute("username");
            orderService.updateOrderStatus(id, status, user);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng sang: " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/detail/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              @RequestParam(value = "reason", defaultValue = "Khách hàng yêu cầu hủy đơn") String reason,
                              HttpSession session,
                              RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        if (!isAdmin && !isStaff) {
            if (!order.getAccount().getId().equals(accountId)) {
                return "redirect:/access-denied";
            }
            if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                ra.addFlashAttribute("error", "Khách hàng chỉ có thể tự hủy đơn khi chưa thanh toán/xuất kho! Vui lòng gửi yêu cầu Hoàn tiền (RMA).");
                return "redirect:/orders/detail/" + id;
            }
        }

        try {
            String user = (String) session.getAttribute("username");
            orderService.cancelOrder(id, reason, user);
            ra.addFlashAttribute("success", "Đã hủy đơn hàng và hoàn trả tồn kho dự trữ!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/detail/" + id;
    }

    @PostMapping("/orders/{id}/shipment")
    public String createShipment(@PathVariable("id") Long id,
                                 @RequestParam("carrier") String carrier,
                                 @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                                 @RequestParam(value = "estimatedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate estimatedDate,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) return "redirect:/access-denied";

        try {
            String user = (String) session.getAttribute("username");
            orderService.createShipment(id, carrier, trackingNumber, estimatedDate, user);
            ra.addFlashAttribute("success", "Đã tạo vận đơn giao hàng (Shipment) thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/detail/" + id;
    }

    @PostMapping("/orders/shipment/{sid}/status")
    public String updateShipmentStatus(@PathVariable("sid") Long sid,
                                       @RequestParam("status") String status,
                                       @RequestParam(value = "redirectOrderId", required = false) Long redirectOrderId,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) return "redirect:/access-denied";

        try {
            String user = (String) session.getAttribute("username");
            orderService.updateShipmentStatus(sid, status, user);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái vận đơn: " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectOrderId != null ? "redirect:/orders/detail/" + redirectOrderId : "redirect:/fulfillment";
    }

    @PostMapping("/orders/{id}/appointment")
    public String createAppointment(@PathVariable("id") Long id,
                                    @RequestParam("serviceName") String serviceName,
                                    @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime,
                                    @RequestParam("location") String location,
                                    @RequestParam(value = "staffAssigned", required = false) String staffAssigned,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) return "redirect:/access-denied";

        try {
            String user = (String) session.getAttribute("username");
            orderService.createAppointment(id, serviceName, appointmentTime, location, staffAssigned, notes, user);
            ra.addFlashAttribute("success", "Đã tạo lịch hẹn dịch vụ (Appointment) thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/detail/" + id;
    }

    @PostMapping("/orders/appointment/{aid}/status")
    public String updateAppointmentStatus(@PathVariable("aid") Long aid,
                                          @RequestParam("status") String status,
                                          @RequestParam(value = "redirectOrderId", required = false) Long redirectOrderId,
                                          HttpSession session,
                                          RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) return "redirect:/access-denied";

        try {
            String user = (String) session.getAttribute("username");
            orderService.updateAppointmentStatus(aid, status, user);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái lịch hẹn: " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectOrderId != null ? "redirect:/orders/detail/" + redirectOrderId : "redirect:/fulfillment";
    }

    @GetMapping("/fulfillment")
    public String fulfillmentOverview(Model model) {
        model.addAttribute("shipments", orderService.findAllShipments());
        model.addAttribute("appointments", orderService.findAllAppointments());
        return "order/fulfillment";
    }
}
