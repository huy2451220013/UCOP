package com.ucop.controller;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.Order;
import com.ucop.entity.Payment;
import com.ucop.enums.PaymentMethod;
import com.ucop.enums.PaymentStatus;
import com.ucop.service.OrderService;
import com.ucop.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout/{orderId}")
    public String checkoutPage(@PathVariable("orderId") Long orderId,
                               @RequestParam(value = "method", defaultValue = "BANK_TRANSFER") PaymentMethod method,
                               @RequestParam(value = "useWallet", defaultValue = "false") boolean useWallet,
                               HttpSession session,
                               Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        if (!isAdmin && !isStaff && !order.getAccount().getId().equals(accountId)) {
            return "redirect:/access-denied";
        }

        Optional<Payment> existingOpt = paymentService.findByOrderId(orderId);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == PaymentStatus.COMPLETED) {
            return "redirect:/payment/invoice/" + existingOpt.get().getId();
        }

        PaymentCalculationDTO calc = paymentService.previewPayment(orderId, method, useWallet);
        model.addAttribute("order", order);
        model.addAttribute("calc", calc);
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("selectedMethod", method);
        model.addAttribute("useWallet", useWallet);
        return "payment/checkout";
    }

    @PostMapping("/process")
    public String processPayment(@RequestParam("orderId") Long orderId,
                                 @RequestParam("method") PaymentMethod method,
                                 @RequestParam(value = "useWallet", defaultValue = "false") boolean useWallet,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        if (!isAdmin && !isStaff && !order.getAccount().getId().equals(accountId)) {
            return "redirect:/access-denied";
        }

        try {
            String user = (String) session.getAttribute("username");
            Payment payment = paymentService.executePayment(orderId, method, useWallet, user);
            ra.addFlashAttribute("success", "Thanh toán thành công! Mã giao dịch: " + payment.getTransactionRef());
            return "redirect:/payment/invoice/" + payment.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/checkout/" + orderId + "?method=" + method + "&useWallet=" + useWallet;
        }
    }

    @GetMapping("/invoice/{paymentId}")
    public String invoice(@PathVariable("paymentId") Long paymentId, HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Payment payment = paymentService.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn thanh toán!"));
        if (!isAdmin && !isStaff && !payment.getOrder().getAccount().getId().equals(accountId)) {
            return "redirect:/access-denied";
        }

        model.addAttribute("payment", payment);
        model.addAttribute("order", payment.getOrder());
        return "payment/invoice";
    }

    @GetMapping("/wallet")
    public String viewWallet(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        model.addAttribute("wallet", paymentService.getOrCreateWallet(accountId));
        model.addAttribute("transactions", paymentService.getWalletTransactions(accountId));
        return "payment/wallet";
    }

    @PostMapping("/wallet/deposit")
    public String depositWallet(@RequestParam("amount") BigDecimal amount,
                                HttpSession session,
                                RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        String user = (String) session.getAttribute("username");
        try {
            paymentService.depositToWallet(accountId, amount, user);
            ra.addFlashAttribute("success", "Nạp tiền vào ví thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payment/wallet";
    }

    @PostMapping("/refund/request")
    public String requestRefund(@RequestParam("orderId") Long orderId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("refundType") String refundType,
                                @RequestParam("reason") String reason,
                                HttpSession session,
                                RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        if (!isAdmin && !isStaff && !order.getAccount().getId().equals(accountId)) {
            return "redirect:/access-denied";
        }

        try {
            String user = (String) session.getAttribute("username");
            paymentService.requestRefund(orderId, amount, refundType, reason, user);
            ra.addFlashAttribute("success", "Đã gửi yêu cầu hoàn tiền (" + refundType + ") thành công! Chờ Nhân viên duyệt.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/detail/" + orderId;
    }

    @GetMapping("/refunds")
    public String listRefunds(Model model) {
        model.addAttribute("refunds", paymentService.findAllRefunds());
        return "payment/refunds";
    }

    @PostMapping("/refund/{id}/process")
    public String processRefund(@PathVariable("id") Long id,
                                @RequestParam("approve") boolean approve,
                                HttpSession session,
                                RedirectAttributes ra) {
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = Boolean.TRUE.equals(session.getAttribute("isStaff"));
        if (!isAdmin && !isStaff) return "redirect:/access-denied";

        try {
            String user = (String) session.getAttribute("username");
            paymentService.processRefund(id, approve, user);
            ra.addFlashAttribute("success", approve
                    ? "Đã DUYỆT hoàn tiền thành công! Số tiền đã được cộng tự động vào Ví (Wallet) của khách hàng."
                    : "Đã TỪ CHỐI yêu cầu hoàn tiền!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payment/refunds";
    }
}
