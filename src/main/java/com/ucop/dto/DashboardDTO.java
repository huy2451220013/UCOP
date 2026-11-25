package com.ucop.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardDTO {
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long totalOrders;
    private long paidOrDeliveredOrders;
    private long canceledOrRefundedOrders;
    private double cancelRefundRate;
    private long totalCustomers;
    private long totalItems;
    private long lowStockCount;
    private long pendingRefundCount;

    // ChartJS series
    private List<String> dailyLabels = new ArrayList<>();
    private List<BigDecimal> dailyRevenue = new ArrayList<>();

    private List<String> monthlyLabels = new ArrayList<>();
    private List<BigDecimal> monthlyRevenue = new ArrayList<>();

    private List<String> methodLabels = new ArrayList<>();
    private List<BigDecimal> methodRevenue = new ArrayList<>();

    private List<String> statusLabels = new ArrayList<>();
    private List<Long> statusCounts = new ArrayList<>();

    private List<Map<String, Object>> topSellingItems = new ArrayList<>();
    private List<Map<String, Object>> warehouseStockSummary = new ArrayList<>();

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getPaidOrDeliveredOrders() { return paidOrDeliveredOrders; }
    public void setPaidOrDeliveredOrders(long paidOrDeliveredOrders) { this.paidOrDeliveredOrders = paidOrDeliveredOrders; }
    public long getCanceledOrRefundedOrders() { return canceledOrRefundedOrders; }
    public void setCanceledOrRefundedOrders(long canceledOrRefundedOrders) { this.canceledOrRefundedOrders = canceledOrRefundedOrders; }
    public double getCancelRefundRate() { return cancelRefundRate; }
    public void setCancelRefundRate(double cancelRefundRate) { this.cancelRefundRate = cancelRefundRate; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public long getPendingRefundCount() { return pendingRefundCount; }
    public void setPendingRefundCount(long pendingRefundCount) { this.pendingRefundCount = pendingRefundCount; }
    public List<String> getDailyLabels() { return dailyLabels; }
    public void setDailyLabels(List<String> dailyLabels) { this.dailyLabels = dailyLabels; }
    public List<BigDecimal> getDailyRevenue() { return dailyRevenue; }
    public void setDailyRevenue(List<BigDecimal> dailyRevenue) { this.dailyRevenue = dailyRevenue; }
    public List<String> getMonthlyLabels() { return monthlyLabels; }
    public void setMonthlyLabels(List<String> monthlyLabels) { this.monthlyLabels = monthlyLabels; }
    public List<BigDecimal> getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(List<BigDecimal> monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    public List<String> getMethodLabels() { return methodLabels; }
    public void setMethodLabels(List<String> methodLabels) { this.methodLabels = methodLabels; }
    public List<BigDecimal> getMethodRevenue() { return methodRevenue; }
    public void setMethodRevenue(List<BigDecimal> methodRevenue) { this.methodRevenue = methodRevenue; }
    public List<String> getStatusLabels() { return statusLabels; }
    public void setStatusLabels(List<String> statusLabels) { this.statusLabels = statusLabels; }
    public List<Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(List<Long> statusCounts) { this.statusCounts = statusCounts; }
    public List<Map<String, Object>> getTopSellingItems() { return topSellingItems; }
    public void setTopSellingItems(List<Map<String, Object>> topSellingItems) { this.topSellingItems = topSellingItems; }
    public List<Map<String, Object>> getWarehouseStockSummary() { return warehouseStockSummary; }
    public void setWarehouseStockSummary(List<Map<String, Object>> warehouseStockSummary) { this.warehouseStockSummary = warehouseStockSummary; }
}
