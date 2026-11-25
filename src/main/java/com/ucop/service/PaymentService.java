package com.ucop.service;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.Payment;
import com.ucop.entity.Refund;
import com.ucop.entity.Wallet;
import com.ucop.entity.WalletTransaction;
import com.ucop.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentService {
    PaymentCalculationDTO previewPayment(Long orderId, PaymentMethod method, boolean useWallet);
    Payment executePayment(Long orderId, PaymentMethod method, boolean useWallet, String performedBy);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByOrderId(Long orderId);

    // Refunds (Full / Partial)
    Refund requestRefund(Long orderId, BigDecimal amount, String refundType, String reason, String requestedBy);
    Refund processRefund(Long refundId, boolean approve, String processedBy);
    List<Refund> findAllRefunds();
    List<Refund> findRefundsByOrder(Long orderId);

    // Wallet
    Wallet getOrCreateWallet(Long accountId);
    Wallet depositToWallet(Long accountId, BigDecimal amount, String performedBy);
    List<WalletTransaction> getWalletTransactions(Long accountId);
}
