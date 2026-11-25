# UCOP - Universal Commerce & Operations Platform (Final Test)

Dự án giải đề **Final Test (100 điểm - 5 Modules)** môn Lập trình Java Nâng Cao.

## 1. Kiến trúc & Công nghệ sử dụng
- **Kiến trúc 3 tầng (3-Layer Architecture):** `Repository (DAO) -> Service (@Transactional) -> Controller -> Thymeleaf View`
- **Ngôn ngữ & Framework:** Java 17+, Spring Boot 3.4 (Spring MVC + Thymeleaf + Embedded Tomcat), Hibernate / Spring Data JPA, Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@Pattern`, `@DecimalMin`, `@Range`)
- **Cơ sở dữ liệu:** Microsoft SQL Server (`ucop_db`, hỗ trợ Unicode tiếng Việt `NVARCHAR`)
- **Thư viện bổ trợ:** `jBCrypt` (mã hóa mật khẩu), `OpenCSV` (Import/Export CSV), `Bootstrap 5.3` & `Chart.js` (Biểu đồ báo cáo trực quan)

---

## 2. Cách chạy dự án nhanh nhất (3 cách)

> **Lưu ý quan trọng:** File `pom.xml` là file cấu hình thư viện của Maven, **không phải** trang web HTML (nếu mở trực tiếp `pom.xml` bằng Microsoft Edge sẽ chỉ hiện cây XML). Để chạy ứng dụng web, hãy làm theo **Cách 1** bên dưới rồi truy cập `http://localhost:8088`.

### Cách 1: Click đúp 1 chạm (Khuyên dùng - Đơn giản nhất)
- Mở thư mục `D:\JAVAnc\UCOP` và click đúp vào file **`run.bat`** (hoặc `D:\JAVAnc\run-ucop.bat`).
- Cửa sổ dòng lệnh sẽ tự khởi động Server trên cổng `8088` (tránh xung đột với XAMPP Apache đang chiếm cổng `8080`) và tự động mở trình duyệt tại địa chỉ:
  **http://localhost:8088**

### Cách 2: Chạy bằng Terminal / Command Prompt / PowerShell
```bat
cd /d D:\JAVAnc\UCOP
run.bat
```
Hoặc chạy trực tiếp qua Maven Wrapper tích hợp sẵn (không cần cài Maven ngoài):
```bat
cd /d D:\JAVAnc\UCOP
mvnw.cmd spring-boot:run
```

### Cách 3: Chạy trong IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Chọn **File -> Open** và mở thư mục **`D:\JAVAnc\UCOP`** (nhận diện là Maven Project).
2. Mở file `src/main/java/com/ucop/UcopApplication.java`.
3. Bấm nút **Run (`▶`)** tại hàm `main()`, sau đó mở trình duyệt vào **http://localhost:8088**.

---

## 3. Tài khoản Demo được khởi tạo tự động (`DataSeeder`)

| Vai trò (RBAC) | Username | Mật khẩu | Quyền hạn chính |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `123456` | Toàn quyền hệ thống + Quản lý User/Role/Khóa TK + Xem Audit Log + Dashboard |
| **STAFF** | `staff` | `123456` | Quản lý Danh mục/SP (CSV), Kho & Low Stock, Đơn hàng, Vận đơn/Lịch hẹn, Duyệt Hoàn tiền, Khuyến mãi, Dashboard |
| **CUSTOMER** | `customer1` | `123456` | Mua sắm, Giỏ hàng, Áp mã KM, Đặt hàng, Thanh toán (COD/Bank/Gateway/Ví UCOP), Gửi yêu cầu Hoàn tiền |
| **CUSTOMER** | `customer2` | `123456` | Tài khoản khách hàng mẫu thứ 2 (đã có sẵn số dư Ví UCOP & đơn hàng mẫu) |

---

## 4. Bản đồ kiểm thử 5 Module theo đề bài (100 điểm)

1. **SV1 – Admin & RBAC (20đ):**
   - Đăng nhập / Đăng xuất / Đăng ký (`Bean Validation`) / Hồ sơ `1-1 Account <-> AccountProfile` / Đổi mật khẩu (`BCrypt`): `/login`, `/register`, `/profile`
   - Quản lý tài khoản, gán quyền `ADMIN`/`STAFF`/`CUSTOMER`, Khóa/Mở khóa tài khoản: `/admin/users`
   - Xem lịch sử thay đổi `AuditLog` (đổi giá, tồn kho, trạng thái đơn, hoàn tiền...): `/admin/audit`
2. **SV2 – Catalog & Stock (20đ):**
   - Danh mục đa cấp `1-N` tự tham chiếu + **Import/Export CSV** (mẫu sẵn tại `sample-csv/categories-sample.csv`): `/categories`
   - Sản phẩm vật lý (`PHYSICAL`) & Dịch vụ (`SERVICE`) + **Import/Export CSV** (`sample-csv/items-sample.csv`): `/items`
   - Quản lý Đa kho (`Warehouse`), tồn kho `OnHand` / `Reserved` / `Available` & cảnh báo tự động **Low Stock**: `/stock`
3. **SV3 – Order & Fulfillment (20đ):**
   - Giỏ hàng, cập nhật số lượng, áp mã giảm giá mẫu (`UCOP10`, `GIAM200K`, `IOT20`), tính tạm tính: `/cart`
   - Vòng đời đơn hàng chuẩn (`PLACED -> PAID -> PACKED -> SHIPPED -> DELIVERED -> CLOSED`, `CANCELED`, `RMA_REQUESTED -> REFUNDED`) & lọc theo **Trạng thái + Khoảng ngày + Khách hàng**: `/orders`
   - Điều phối tách biệt `1-N Shipment` (hàng vật lý) & `1-N Appointment` (lịch hẹn dịch vụ): `/orders/detail/{id}` và `/fulfillment`
4. **SV4 – Payment, Refund & Wallet (20đ):**
   - Tự động tính `Subtotal`, `ItemDiscount`, `CartDiscount`, `VAT 10%`, `ShippingFee` (theo kg), `CODFee (15,000đ)`, `GatewayFee (1.5%)`, kết hợp trừ **Ví UCOP**: `/payment/checkout/{orderId}`
   - In Hóa đơn / Biên lai thanh toán: `/payment/invoice/{paymentId}`
   - Ví điện tử nội bộ (Nạp tiền & lịch sử `DEPOSIT` / `PAYMENT` / `REFUND`): `/payment/wallet`
   - Hoàn tiền Toàn phần (`FULL`) & Một phần (`PARTIAL`) tự động cộng tiền về Ví khách hàng: `/payment/refunds`
5. **SV5 – Promotion, Report & Dashboard (20đ):**
   - Khuyến mãi **Item-level** & **Cart-level**, giảm `%` hoặc `VNĐ`, giới hạn ngày & lượt dùng + lịch sử `PromotionUsage`: `/promotions`
   - Dashboard thống kê SQL Server + 4 biểu đồ **Chart.js** (Doanh thu ngày, Doanh thu tháng, Phương thức thanh toán, Trạng thái đơn, Top bán chạy, Tồn kho & Tỷ lệ Hủy/Hoàn): `/reports/dashboard`
