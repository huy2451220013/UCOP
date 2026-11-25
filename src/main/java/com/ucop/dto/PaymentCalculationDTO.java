package com.ucop.dto;

import com.ucop.enums.PaymentMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentCalculationDTO {
    private Long orderId;
    private PaymentMethod method;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal itemDiscount = BigDecimal.ZERO;
    private BigDecimal cartDiscount = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal codFee = BigDecimal.ZERO;
    private BigDecimal gatewayFee = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private BigDecimal walletUsed = BigDecimal.ZERO;
    private BigDecimal amountDue = BigDecimal.ZERO;

    public static PaymentCalculationDTO calculate(
            BigDecimal subtotal,
            BigDecimal itemDiscount,
            BigDecimal cartDiscount,
            double totalPhysicalWeightKg,
            boolean hasPhysicalItems,
            PaymentMethod method,
            BigDecimal walletBalance,
            boolean useWallet) {

        PaymentCalculationDTO dto = new PaymentCalculationDTO();
        dto.setMethod(method);
        dto.setSubtotal(subtotal != null ? subtotal : BigDecimal.ZERO);
        dto.setItemDiscount(itemDiscount != null ? itemDiscount : BigDecimal.ZERO);
        dto.setCartDiscount(cartDiscount != null ? cartDiscount : BigDecimal.ZERO);

        BigDecimal totalDisc = dto.getItemDiscount().add(dto.getCartDiscount());
        if (totalDisc.compareTo(dto.getSubtotal()) > 0) {
            totalDisc = dto.getSubtotal();
        }
        dto.setTotalDiscount(totalDisc);

        BigDecimal taxable = dto.getSubtotal().subtract(totalDisc).max(BigDecimal.ZERO);
        BigDecimal vat = taxable.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
        dto.setTaxAmount(vat);

        BigDecimal ship = BigDecimal.ZERO;
        if (hasPhysicalItems) {
            // Co ban 25,000d cho 2kg dau, moi kg tiep theo +5,000d
            double extraKg = Math.max(0.0, Math.ceil(totalPhysicalWeightKg - 2.0));
            ship = new BigDecimal("25000").add(BigDecimal.valueOf((long) extraKg * 5000L));
        }
        dto.setShippingFee(ship);

        BigDecimal cod = (method == PaymentMethod.COD && hasPhysicalItems)
                ? new BigDecimal("15000") : BigDecimal.ZERO;
        dto.setCodFee(cod);

        BigDecimal gw = (method == PaymentMethod.GATEWAY)
                ? taxable.multiply(new BigDecimal("0.015")).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        dto.setGatewayFee(gw);

        BigDecimal grand = taxable.add(vat).add(ship).add(cod).add(gw);
        dto.setGrandTotal(grand);

        BigDecimal wBal = walletBalance != null ? walletBalance : BigDecimal.ZERO;
        dto.setWalletBalance(wBal);

        BigDecimal wUsed = BigDecimal.ZERO;
        if (method == PaymentMethod.WALLET || useWallet) {
            wUsed = wBal.min(grand);
        }
        dto.setWalletUsed(wUsed);
        dto.setAmountDue(grand.subtract(wUsed).max(BigDecimal.ZERO));
        return dto;
    }

    public static BigDecimal calculateShippingByWeight(double totalPhysicalWeightKg, boolean hasPhysicalItems) {
        if (!hasPhysicalItems) return BigDecimal.ZERO;
        double extraKg = Math.max(0.0, Math.ceil(totalPhysicalWeightKg - 2.0));
        return new BigDecimal("25000").add(BigDecimal.valueOf((long) extraKg * 5000L));
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getItemDiscount() { return itemDiscount; }
    public void setItemDiscount(BigDecimal itemDiscount) { this.itemDiscount = itemDiscount; }
    public BigDecimal getCartDiscount() { return cartDiscount; }
    public void setCartDiscount(BigDecimal cartDiscount) { this.cartDiscount = cartDiscount; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getCodFee() { return codFee; }
    public void setCodFee(BigDecimal codFee) { this.codFee = codFee; }
    public BigDecimal getGatewayFee() { return gatewayFee; }
    public void setGatewayFee(BigDecimal gatewayFee) { this.gatewayFee = gatewayFee; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public BigDecimal getWalletUsed() { return walletUsed; }
    public void setWalletUsed(BigDecimal walletUsed) { this.walletUsed = walletUsed; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
}
