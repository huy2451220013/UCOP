package com.ucop.service.impl;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.*;
import com.ucop.enums.*;
import com.ucop.repository.*;
import com.ucop.service.AuditService;
import com.ucop.service.PaymentService;
import com.ucop.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final AccountRepository accountRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              RefundRepository refundRepository,
                              WalletRepository walletRepository,
                              WalletTransactionRepository walletTxRepository,
                              AccountRepository accountRepository,
                              StockService stockService,
                              AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
        this.accountRepository = accountRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCalculationDTO previewPayment(Long orderId, PaymentMethod method, boolean useWallet) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        double totalWeight = order.getOrderItems().stream()
                .filter(oi -> oi.getItem().getItemType() == ItemType.PHYSICAL)
                .mapToDouble(oi -> (oi.getItem().getWeight() != null ? oi.getItem().getWeight().doubleValue() : 0.5) * oi.getQuantity())
                .sum();
        boolean hasPhysical = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getItem().getItemType() == ItemType.PHYSICAL);

        BigDecimal walletBal = walletRepository.findByAccountId(order.getAccount().getId())
                .map(Wallet::getBalance).orElse(BigDecimal.ZERO);

        PaymentCalculationDTO calc = PaymentCalculationDTO.calculate(
                order.getSubtotal(),
                order.getItemDiscount(),
                order.getCartDiscount(),
                totalWeight,
                hasPhysical,
                method,
                walletBal,
                useWallet
        );
        calc.setOrderId(orderId);
        return calc;
    }

    @Override
    public Payment executePayment(Long orderId, PaymentMethod method, boolean useWallet, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        PaymentCalculationDTO calc = previewPayment(orderId, method, useWallet);
        Wallet wallet = getOrCreateWallet(order.getAccount().getId());

        if (method == PaymentMethod.WALLET && calc.getAmountDue().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Số dư Ví UCOP không đủ để thanh toán toàn bộ đơn hàng! Vui lòng nạp thêm hoặc chọn phương thức kết hợp.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setSubtotal(calc.getSubtotal());
        payment.setItemDiscount(calc.getItemDiscount());
        payment.setCartDiscount(calc.getCartDiscount());
        payment.setDiscountAmount(calc.getTotalDiscount());
        payment.setTaxAmount(calc.getTaxAmount());
        payment.setShippingFee(calc.getShippingFee());
        payment.setCodFee(calc.getCodFee());
        payment.setGatewayFee(calc.getGatewayFee());
        payment.setGrandTotal(calc.getGrandTotal());
        payment.setWalletUsed(calc.getWalletUsed());
        payment.setAmountDue(calc.getAmountDue());
        payment.setAmountPaid(calc.getGrandTotal());
        payment.setTransactionRef("TXN-" + method.name() + "-" + (System.currentTimeMillis() % 100000000));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        // Deduct wallet if used
        if (calc.getWalletUsed().compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalance(wallet.getBalance().subtract(calc.getWalletUsed()));
            wallet.setTotalSpent(wallet.getTotalSpent().add(calc.getWalletUsed()));
            walletRepository.save(wallet);
            walletTxRepository.save(new WalletTransaction(wallet, "PAYMENT",
                    calc.getWalletUsed().negate(), "Thanh toán đơn hàng " + order.getOrderCode()));
        }

        // Confirm stock deduction from reserved -> sold
        for (OrderItem oi : order.getOrderItems()) {
            stockService.confirmDeductReservedStock(oi.getItem().getId(), oi.getQuantity(), performedBy);
        }

        // Sync order totals & status
        order.setTaxAmount(calc.getTaxAmount());
        order.setShippingFee(calc.getShippingFee());
        order.setGrandTotal(calc.getGrandTotal());
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedBy(performedBy);
        orderRepository.save(order);

        Payment saved = paymentRepository.save(payment);

        auditService.log("PAYMENT_COMPLETED", "Payment", saved.getId(), performedBy,
                OrderStatus.PLACED.name(), OrderStatus.PAID.name(),
                "Thanh toán đơn " + order.getOrderCode() + " qua " + method + " (" + saved.getGrandTotal().toPlainString() + " đ)");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    public Refund requestRefund(Long orderId, BigDecimal amount, String refundType, String reason, String requestedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        BigDecimal maxRefundable = order.getGrandTotal();
        BigDecimal finalAmount = "FULL".equalsIgnoreCase(refundType) ? maxRefundable : amount;
        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0 || finalAmount.compareTo(maxRefundable) > 0) {
            throw new IllegalArgumentException("Số tiền yêu cầu hoàn phải lớn hơn 0 và không vượt quá tổng đơn (" + maxRefundable.toPlainString() + " đ)!");
        }

        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setPayment(payment);
        refund.setAmount(finalAmount);
        refund.setRefundType(finalAmount.compareTo(maxRefundable) == 0 ? "FULL" : "PARTIAL");
        refund.setReason(reason);
        refund.setStatus("PENDING");
        refund.setRequestedBy(requestedBy);

        order.setStatus(OrderStatus.RMA_REQUESTED);
        order.setUpdatedBy(requestedBy);
        orderRepository.save(order);

        Refund saved = refundRepository.save(refund);
        auditService.log("REQUEST_REFUND", "Refund", saved.getId(), requestedBy,
                null, finalAmount.toPlainString(), "Yêu cầu hoàn tiền (" + refund.getRefundType() + ") cho đơn " + order.getOrderCode());
        return saved;
    }

    @Override
    public Refund processRefund(Long refundId, boolean approve, String processedBy) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền!"));

        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            throw new IllegalStateException("Yêu cầu hoàn tiền này đã được xử lý trước đó!");
        }

        refund.setProcessedBy(processedBy);
        refund.setProcessedAt(LocalDateTime.now());
        Order order = refund.getOrder();

        if (approve) {
            refund.setStatus("APPROVED");
            order.setStatus(OrderStatus.REFUNDED);
            order.setUpdatedBy(processedBy);
            orderRepository.save(order);

            if (refund.getPayment() != null) {
                Payment p = refund.getPayment();
                p.setStatus("FULL".equalsIgnoreCase(refund.getRefundType())
                        ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
                paymentRepository.save(p);
            }

            // Hoàn tiền trực tiếp vào Ví nội bộ của khách hàng
            Wallet wallet = getOrCreateWallet(order.getAccount().getId());
            wallet.setBalance(wallet.getBalance().add(refund.getAmount()));
            walletRepository.save(wallet);
            walletTxRepository.save(new WalletTransaction(wallet, "REFUND",
                    refund.getAmount(), "Hoàn tiền (" + refund.getRefundType() + ") đơn hàng " + order.getOrderCode()));

            auditService.log("APPROVE_REFUND", "Refund", refund.getId(), processedBy,
                    "PENDING", "APPROVED", "Đã hoàn " + refund.getAmount().toPlainString() + " đ vào ví khách " + order.getAccount().getUsername());
        } else {
            refund.setStatus("REJECTED");
            order.setStatus(OrderStatus.DELIVERED);
            order.setUpdatedBy(processedBy);
            orderRepository.save(order);
            auditService.log("REJECT_REFUND", "Refund", refund.getId(), processedBy,
                    "PENDING", "REJECTED", "Từ chối hoàn tiền đơn " + order.getOrderCode());
        }

        return refundRepository.save(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> findAllRefunds() {
        return refundRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Refund> findRefundsByOrder(Long orderId) {
        return refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Override
    public Wallet getOrCreateWallet(Long accountId) {
        return walletRepository.findByAccountId(accountId).orElseGet(() -> {
            Account acc = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
            Wallet w = new Wallet();
            w.setAccount(acc);
            w.setBalance(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    @Override
    public Wallet depositToWallet(Long accountId, BigDecimal amount, String performedBy) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            throw new IllegalArgumentException("Số tiền nạp tối thiểu là 10,000 VND!");
        }
        Wallet wallet = getOrCreateWallet(accountId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalDeposited(wallet.getTotalDeposited().add(amount));
        Wallet saved = walletRepository.save(wallet);

        walletTxRepository.save(new WalletTransaction(saved, "DEPOSIT", amount, "Nạp tiền vào ví UCOP Wallet"));
        auditService.log("WALLET_DEPOSIT", "Wallet", saved.getId(), performedBy,
                null, amount.toPlainString(), "Nạp tiền ví: +" + amount.toPlainString() + " đ");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getWalletTransactions(Long accountId) {
        Wallet wallet = getOrCreateWallet(accountId);
        return walletTxRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
