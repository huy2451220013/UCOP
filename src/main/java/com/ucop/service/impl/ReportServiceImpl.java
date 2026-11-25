package com.ucop.service.impl;

import com.ucop.dto.DashboardDTO;
import com.ucop.entity.*;
import com.ucop.enums.*;
import com.ucop.repository.*;
import com.ucop.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockItemRepository stockItemRepository;

    public ReportServiceImpl(OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             PaymentRepository paymentRepository,
                             RefundRepository refundRepository,
                             AccountRepository accountRepository,
                             ItemRepository itemRepository,
                             WarehouseRepository warehouseRepository,
                             StockItemRepository stockItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockItemRepository = stockItemRepository;
    }

    @Override
    public DashboardDTO buildDashboard() {
        DashboardDTO dto = new DashboardDTO();

        List<PaymentStatus> revenueStatuses = List.of(PaymentStatus.COMPLETED, PaymentStatus.PARTIALLY_REFUNDED);
        BigDecimal totalRev = paymentRepository.sumRevenueByStatuses(revenueStatuses);
        dto.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);

        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        dto.setTotalOrders(totalOrders);

        long successCount = orderRepository.countByStatusIn(List.of(
                OrderStatus.PAID, OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CLOSED));
        long cancelRefundCount = orderRepository.countByStatusIn(List.of(
                OrderStatus.CANCELED, OrderStatus.RMA_REQUESTED, OrderStatus.REFUNDED));

        dto.setPaidOrDeliveredOrders(successCount);
        dto.setCanceledOrRefundedOrders(cancelRefundCount);

        if (totalOrders > 0) {
            double rate = BigDecimal.valueOf(cancelRefundCount * 100.0 / totalOrders)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            dto.setCancelRefundRate(rate);
        }

        dto.setTotalCustomers(accountRepository.countByRole(UserRole.CUSTOMER));
        dto.setTotalItems(itemRepository.count());
        dto.setLowStockCount(stockItemRepository.findLowStockItems().size());
        dto.setPendingRefundCount(refundRepository.countByStatus("PENDING"));

        // 1. Revenue by Day (Last 7 days)
        List<Payment> completedPayments = paymentRepository.findByStatusIn(revenueStatuses);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            BigDecimal sum = completedPayments.stream()
                    .filter(p -> p.getPaidAt() != null && p.getPaidAt().toLocalDate().equals(d))
                    .map(Payment::getGrandTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.getDailyLabels().add(d.format(dayFmt));
            dto.getDailyRevenue().add(sum);
        }

        // 2. Revenue by Month (Last 6 months)
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            BigDecimal sum = completedPayments.stream()
                    .filter(p -> p.getPaidAt() != null && YearMonth.from(p.getPaidAt()).equals(ym))
                    .map(Payment::getGrandTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.getMonthlyLabels().add(ym.format(monthFmt));
            dto.getMonthlyRevenue().add(sum);
        }

        // 3. Revenue by Payment Method
        Map<PaymentMethod, BigDecimal> methodMap = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod pm : PaymentMethod.values()) methodMap.put(pm, BigDecimal.ZERO);
        for (Object[] row : paymentRepository.sumRevenueByMethod(revenueStatuses)) {
            PaymentMethod pm = (PaymentMethod) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            methodMap.put(pm, amt);
        }
        methodMap.forEach((k, v) -> {
            dto.getMethodLabels().add(k.name());
            dto.getMethodRevenue().add(v);
        });

        // 4. Order Status Breakdown
        Map<String, Long> statusGroup = new LinkedHashMap<>();
        statusGroup.put("Đã thanh toán / Đang xử lý", orderRepository.countByStatusIn(List.of(OrderStatus.PAID, OrderStatus.PACKED, OrderStatus.SHIPPED)));
        statusGroup.put("Hoàn thành (Delivered/Closed)", orderRepository.countByStatusIn(List.of(OrderStatus.DELIVERED, OrderStatus.CLOSED)));
        statusGroup.put("Chờ thanh toán (Placed)", orderRepository.countByStatusIn(List.of(OrderStatus.PLACED, OrderStatus.PENDING_PAYMENT)));
        statusGroup.put("Hủy / Hoàn tiền", cancelRefundCount);
        statusGroup.forEach((k, v) -> {
            dto.getStatusLabels().add(k);
            dto.getStatusCounts().add(v);
        });

        // 5. Top Selling Items
        List<Object[]> topRows = orderItemRepository.findTopSellingItems(List.of(OrderStatus.CANCELED, OrderStatus.REFUNDED));
        int limit = Math.min(8, topRows.size());
        for (int i = 0; i < limit; i++) {
            Object[] r = topRows.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("sku", r[0]);
            m.put("name", r[1]);
            m.put("itemType", r[2]);
            m.put("totalQty", r[3]);
            m.put("totalRevenue", r[4]);
            dto.getTopSellingItems().add(m);
        }

        // 6. Stock Summary by Warehouse
        for (Warehouse wh : warehouseRepository.findAll()) {
            int totalOnHand = wh.getStockItems().stream().mapToInt(StockItem::getOnHand).sum();
            int totalReserved = wh.getStockItems().stream().mapToInt(StockItem::getReserved).sum();
            long lowCount = wh.getStockItems().stream().filter(StockItem::isLowStock).count();
            Map<String, Object> m = new HashMap<>();
            m.put("code", wh.getCode());
            m.put("name", wh.getName());
            m.put("location", wh.getLocation());
            m.put("skuCount", wh.getStockItems().size());
            m.put("totalOnHand", totalOnHand);
            m.put("totalReserved", totalReserved);
            m.put("lowCount", lowCount);
            dto.getWarehouseStockSummary().add(m);
        }

        return dto;
    }
}
