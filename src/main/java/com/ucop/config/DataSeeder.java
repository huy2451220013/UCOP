package com.ucop.config;

import com.ucop.entity.*;
import com.ucop.enums.*;
import com.ucop.repository.*;
import com.ucop.service.AuditService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockItemRepository stockItemRepository;
    private final PromotionRepository promotionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public DataSeeder(RoleRepository roleRepository,
                      AccountRepository accountRepository,
                      WalletRepository walletRepository,
                      WalletTransactionRepository walletTxRepository,
                      CategoryRepository categoryRepository,
                      ItemRepository itemRepository,
                      WarehouseRepository warehouseRepository,
                      StockItemRepository stockItemRepository,
                      PromotionRepository promotionRepository,
                      OrderRepository orderRepository,
                      PaymentRepository paymentRepository,
                      ShipmentRepository shipmentRepository,
                      AppointmentRepository appointmentRepository,
                      AuditService auditService) {
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockItemRepository = stockItemRepository;
        this.promotionRepository = promotionRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            return; // Đã có dữ liệu
        }

        // 1. Roles
        Role rAdmin = roleRepository.save(new Role(UserRole.ADMIN, "Quản trị viên hệ thống"));
        Role rStaff = roleRepository.save(new Role(UserRole.STAFF, "Nhân viên vận hành & kho"));
        Role rCustomer = roleRepository.save(new Role(UserRole.CUSTOMER, "Khách hàng"));

        // 2. Accounts (Password: 123456)
        Account admin = createSeedAccount("admin", "123456", "admin@ucop.vn", "Nguyễn Quản Trị", "0905111222", " Hải Châu, Đà Nẵng", rAdmin, new BigDecimal("50000000"));
        Account staff = createSeedAccount("staff", "123456", "staff@ucop.vn", "Lê Vận Hành", "0905333444", "Thanh Khê, Đà Nẵng", rStaff, new BigDecimal("10000000"));
        Account cust1 = createSeedAccount("customer1", "123456", "customer1@gmail.com", "Trần Minh Khôi", "0912555666", "128 Nguyễn Văn Linh, Đà Nẵng", rCustomer, new BigDecimal("20000000"));
        Account cust2 = createSeedAccount("customer2", "123456", "customer2@gmail.com", "Phạm Thu Hà", "0988777888", "45 Lê Lợi, Quận 1, TP.HCM", rCustomer, new BigDecimal("8000000"));

        // 3. Multi-level Categories (1-N Parent -> Children)
        Category catRetail = saveCat("Thiết Bị & Bán Lẻ", "Nhóm sản phẩm phần cứng & bán lẻ vật lý", null);
        Category catLaptop = saveCat("Laptop & Máy Trạm", "Máy tính xách tay doanh nghiệp & lập trình", catRetail);
        Category catIot = saveCat("Thiết Bị IoT & Cảm Biến", "Thiết bị nhà thông minh và trạm quan trắc", catRetail);

        Category catService = saveCat("Dịch Vụ, Giáo Dục & Y Tế", "Các gói dịch vụ đặt lịch hẹn không cần vận chuyển vật lý", null);
        Category catCourse = saveCat("Khóa Học Công Nghệ", "Khóa học lập trình Java, Spring, AI", catService);
        Category catMaint = saveCat("Bảo Trì & Khám Tổng Quát", "Gói bảo trì định kỳ IoT & Gói khám sức khỏe", catService);

        // 4. Items (Physical & Service)
        Item i1 = saveItem("LAP-DELL-XPS15", "Laptop Dell XPS 15 OLED", catLaptop, ItemType.PHYSICAL, "Chiếc", new BigDecimal("35900000"), new BigDecimal("2.2"), "Laptop cao cấp màn hình 3.5K OLED, Core i7, RAM 32GB", "/images/dell-xps15.jpg");
        Item i2 = saveItem("LAP-MAC-M3", "MacBook Air M3 13 inch 16GB", catLaptop, ItemType.PHYSICAL, "Chiếc", new BigDecimal("27500000"), new BigDecimal("1.5"), "Chip Apple M3 hiệu năng cao, pin 18 giờ", "/images/macbook-m3.jpg");
        Item i3 = saveItem("IOT-KIT-PRO", "Bộ Cảm Biến Quan Trắc Môi Trường IoT Pro", catIot, ItemType.PHYSICAL, "Bộ", new BigDecimal("4200000"), new BigDecimal("1.2"), "Đo bụi mịn PM2.5, nhiệt độ, độ ẩm kết nối WiFi/LoRa", "/images/iot-kit-pro.jpg");
        Item i4 = saveItem("IOT-CAM-AI", "Camera AI Nhận Diện Thông Minh 4K", catIot, ItemType.PHYSICAL, "Chiếc", new BigDecimal("2850000"), new BigDecimal("0.8"), "Camera an ninh tích hợp AI cảnh báo xâm nhập", "/images/iot-cam-ai.jpg");
        Item i5 = saveItem("EDU-SPRING-PRO", "Khóa Học Master Spring MVC & Hibernate Thực Chiến", catCourse, ItemType.SERVICE, "Khóa", new BigDecimal("2500000"), BigDecimal.ZERO, "Khóa học 12 tuần kèm mentor 1-1 và dự án doanh nghiệp", "/images/edu-spring-pro.jpg");
        Item i6 = saveItem("SRV-IOT-MAINT", "Gói Bảo Trì Trạm IoT Định Kỳ 6 Tháng", catMaint, ItemType.SERVICE, "Gói", new BigDecimal("1800000"), BigDecimal.ZERO, "Kiểm tra, hiệu chuẩn cảm biến tận nơi bởi kỹ sư chuyên trách", "/images/srv-iot-maint.jpg");

        // 5. Warehouses & StockItems (kèm Low Stock <= 5)
        Warehouse whDn = warehouseRepository.save(new Warehouse("WH-DN", "Kho Trung Tâm Đà Nẵng", "KCN Hòa Khánh, Liên Chiểu, Đà Nẵng"));
        Warehouse whHcm = warehouseRepository.save(new Warehouse("WH-HCM", "Kho Phân Phối TP.HCM", "Khu Công Nghệ Cao Quận 9, TP.HCM"));

        saveStock(whDn, i1, 15, 1, 5);
        saveStock(whDn, i2, 4, 1, 5);   // Available = 3 <= 5 -> Cảnh báo Low Stock!
        saveStock(whDn, i3, 25, 0, 5);
        saveStock(whDn, i4, 3, 0, 5);   // Available = 3 <= 5 -> Cảnh báo Low Stock!
        saveStock(whHcm, i1, 10, 0, 5);
        saveStock(whHcm, i3, 18, 2, 5);

        // 6. Promotions (Cart-level & Item-level)
        Promotion p1 = new Promotion();
        p1.setCode("UCOP10");
        p1.setScope(PromotionScope.CART);
        p1.setDiscountType(DiscountType.PERCENTAGE);
        p1.setDiscountValue(new BigDecimal("10"));
        p1.setMinOrderAmount(new BigDecimal("1000000"));
        p1.setMaxUsage(200);
        p1.setUsedCount(2);
        p1.setStartDate(LocalDateTime.now().minusMonths(1));
        p1.setEndDate(LocalDateTime.now().plusMonths(6));
        p1.setDescription("Giảm 10% toàn bộ giỏ hàng cho đơn từ 1,000,000 đ");
        promotionRepository.save(p1);

        Promotion p2 = new Promotion();
        p2.setCode("GIAM200K");
        p2.setScope(PromotionScope.CART);
        p2.setDiscountType(DiscountType.FIXED);
        p2.setDiscountValue(new BigDecimal("200000"));
        p2.setMinOrderAmount(new BigDecimal("2000000"));
        p2.setMaxUsage(100);
        p2.setUsedCount(1);
        p2.setStartDate(LocalDateTime.now().minusMonths(1));
        p2.setEndDate(LocalDateTime.now().plusMonths(6));
        p2.setDescription("Giảm trực tiếp 200,000 đ cho đơn từ 2,000,000 đ");
        promotionRepository.save(p2);

        Promotion p3 = new Promotion();
        p3.setCode("IOT20");
        p3.setScope(PromotionScope.ITEM);
        p3.setTargetItem(i3);
        p3.setDiscountType(DiscountType.PERCENTAGE);
        p3.setDiscountValue(new BigDecimal("20"));
        p3.setMinOrderAmount(BigDecimal.ZERO);
        p3.setMaxUsage(50);
        p3.setUsedCount(1);
        p3.setStartDate(LocalDateTime.now().minusMonths(1));
        p3.setEndDate(LocalDateTime.now().plusMonths(6));
        p3.setDescription("Ưu đãi Item-level: Giảm 20% riêng cho Bộ Cảm Biến IoT Pro");
        promotionRepository.save(p3);

        // 7. Sample Historical Orders + Payments + Shipments + Appointments (for Dashboard Charts)
        createSamplePaidOrder("ORD-SEED-101", cust1, i2, 1, PaymentMethod.BANK_TRANSFER, OrderStatus.DELIVERED, LocalDateTime.now().minusDays(4), true, false);
        createSamplePaidOrder("ORD-SEED-102", cust2, i3, 2, PaymentMethod.GATEWAY, OrderStatus.SHIPPED, LocalDateTime.now().minusDays(2), true, false);
        createSamplePaidOrder("ORD-SEED-103", cust1, i5, 1, PaymentMethod.WALLET, OrderStatus.PAID, LocalDateTime.now().minusDays(1), false, true);
        createSamplePaidOrder("ORD-SEED-104", cust2, i4, 2, PaymentMethod.COD, OrderStatus.PAID, LocalDateTime.now(), true, false);

        auditService.log("SYSTEM_INIT", "System", 1L, admin.getUsername(), null, "READY",
                "Khởi tạo dữ liệu mẫu hệ thống UCOP thành công (" + staff.getUsername() + " sẵn sàng)");
    }

    private Account createSeedAccount(String username, String rawPass, String email, String fullName,
                                      String phone, String address, Role role, BigDecimal walletBal) {
        Account acc = new Account();
        acc.setUsername(username);
        acc.setPassword(BCrypt.hashpw(rawPass, BCrypt.gensalt(10)));
        acc.setEmail(email);
        acc.setStatus("ACTIVE");
        acc.setCreatedBy("SYSTEM");
        acc.setUpdatedBy("SYSTEM");
        acc.getRoles().add(role);
        acc.setProfile(new AccountProfile(fullName, phone, address));
        Account saved = accountRepository.save(acc);

        Wallet w = new Wallet();
        w.setAccount(saved);
        w.setBalance(walletBal);
        w.setTotalDeposited(walletBal);
        Wallet savedW = walletRepository.save(w);
        walletTxRepository.save(new WalletTransaction(savedW, "DEPOSIT", walletBal, "Số dư khởi tạo tài khoản mẫu"));
        return saved;
    }

    private Category saveCat(String name, String desc, Category parent) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(desc);
        c.setParent(parent);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private Item saveItem(String sku, String name, Category cat, ItemType type, String unit,
                          BigDecimal price, BigDecimal weight, String desc, String imageUrl) {
        Item it = new Item();
        it.setSku(sku);
        it.setName(name);
        it.setCategory(cat);
        it.setItemType(type);
        it.setUnit(unit);
        it.setPrice(price);
        it.setWeight(weight);
        it.setStatus("ACTIVE");
        it.setDescription(desc);
        it.setImageUrl(imageUrl);
        it.setCreatedBy("admin");
        it.setUpdatedBy("admin");
        return itemRepository.save(it);
    }

    private void saveStock(Warehouse wh, Item item, int onHand, int reserved, int threshold) {
        StockItem s = new StockItem();
        s.setWarehouse(wh);
        s.setItem(item);
        s.setOnHand(onHand);
        s.setReserved(reserved);
        s.setLowStockThreshold(threshold);
        s.setUpdatedBy("staff");
        stockItemRepository.save(s);
    }

    private void createSamplePaidOrder(String code, Account cust, Item item, int qty,
                                       PaymentMethod method, OrderStatus status, LocalDateTime time,
                                       boolean withShipment, boolean withAppointment) {
        BigDecimal sub = item.getPrice().multiply(BigDecimal.valueOf(qty));
        BigDecimal tax = sub.multiply(new BigDecimal("0.10")).setScale(0);
        BigDecimal ship = item.getItemType() == ItemType.PHYSICAL ? new BigDecimal("25000") : BigDecimal.ZERO;
        BigDecimal cod = (method == PaymentMethod.COD && item.getItemType() == ItemType.PHYSICAL) ? new BigDecimal("15000") : BigDecimal.ZERO;
        BigDecimal gw = method == PaymentMethod.GATEWAY ? sub.multiply(new BigDecimal("0.015")).setScale(0) : BigDecimal.ZERO;
        BigDecimal grand = sub.add(tax).add(ship).add(cod).add(gw);

        Order o = new Order();
        o.setOrderCode(code);
        o.setAccount(cust);
        o.setStatus(status);
        o.setSubtotal(sub);
        o.setTaxAmount(tax);
        o.setShippingFee(ship);
        o.setGrandTotal(grand);
        o.setShippingAddress(cust.getProfile().getAddress());
        o.setNote("Đơn hàng mẫu phục vụ thống kê Dashboard");
        o.setCreatedAt(time);
        o.setCreatedBy(cust.getUsername());
        o.setUpdatedBy("staff");

        OrderItem oi = new OrderItem();
        oi.setOrder(o);
        oi.setItem(item);
        oi.setQuantity(qty);
        oi.setUnitPrice(item.getPrice());
        o.getOrderItems().add(oi);
        Order savedOrder = orderRepository.save(o);

        Payment p = new Payment();
        p.setOrder(savedOrder);
        p.setMethod(method);
        p.setStatus(PaymentStatus.COMPLETED);
        p.setSubtotal(sub);
        p.setTaxAmount(tax);
        p.setShippingFee(ship);
        p.setCodFee(cod);
        p.setGatewayFee(gw);
        p.setGrandTotal(grand);
        p.setAmountDue(method == PaymentMethod.WALLET ? BigDecimal.ZERO : grand);
        p.setWalletUsed(method == PaymentMethod.WALLET ? grand : BigDecimal.ZERO);
        p.setAmountPaid(grand);
        p.setTransactionRef("TXN-SEED-" + code.substring(code.length() - 3));
        p.setPaidAt(time.plusMinutes(15));
        p.setCreatedAt(time);
        paymentRepository.save(p);

        if (withShipment) {
            Shipment sh = new Shipment();
            sh.setOrder(savedOrder);
            sh.setCarrier("GiaoHangNhanh (GHN)");
            sh.setTrackingNumber("GHN" + code.substring(code.length() - 3) + "8899");
            sh.setShippingAddress(savedOrder.getShippingAddress());
            sh.setStatus(status == OrderStatus.DELIVERED ? "DELIVERED" : "IN_TRANSIT");
            sh.setShippedAt(time.plusHours(2));
            sh.setEstimatedDelivery(time.plusDays(2));
            if (status == OrderStatus.DELIVERED) sh.setDeliveredAt(time.plusDays(1));
            shipmentRepository.save(sh);
        }

        if (withAppointment) {
            Appointment ap = new Appointment();
            ap.setOrder(savedOrder);
            ap.setServiceName(item.getName());
            ap.setAppointmentTime(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0));
            ap.setLocation("Trung tâm Đào tạo & Dịch vụ UCOP - Tầng 5, Đà Nẵng");
            ap.setStaffAssigned("Lê Vận Hành (staff)");
            ap.setStatus("CONFIRMED");
            ap.setNotes("Lịch hẹn học/tư vấn trực tiếp");
            appointmentRepository.save(ap);
        }
    }
}
