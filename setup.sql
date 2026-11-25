-- =====================================================================
-- UCOP - Universal Commerce & Operations Platform (Final Test)
-- Database Initialization Script for Microsoft SQL Server
-- =====================================================================
-- Lưu ý:
-- 1. Script này tạo Database [ucop_db] và tài khoản SQL [ucop_user] / [Ucop@123456]
--    khớp 100% với cấu hình trong src/main/resources/application.properties.
-- 2. Khi chạy ứng dụng (run.bat hoặc mvnw spring-boot:run), Hibernate JPA
--    (ddl-auto=update, use_nationalized_character_data=true) sẽ tự động ánh xạ
--    toàn bộ 18 bảng chuẩn NVARCHAR tiếng Việt và DataSeeder sẽ tự động khởi tạo
--    đầy đủ dữ liệu mẫu cho cả 5 Module (SV1 -> SV5) nếu DB đang trống!
-- =====================================================================

USE master;
GO

IF DB_ID(N'ucop_db') IS NULL
BEGIN
    CREATE DATABASE ucop_db;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'ucop_user')
BEGIN
    CREATE LOGIN ucop_user WITH PASSWORD = N'Ucop@123456', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
END
GO

USE ucop_db;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'ucop_user')
BEGIN
    CREATE USER ucop_user FOR LOGIN ucop_user;
END
GO

ALTER ROLE db_owner ADD MEMBER ucop_user;
GO

-- =====================================================================
-- DANH SÁCH 18 BẢNG ORM ĐƯỢC QUẢN LÝ BỞI HIBERNATE / JPA TRONG UCOP:
--   Module SV1 (Admin & RBAC):
--     1. roles (id, role_name)
--     2. accounts (id, username, password, email, status, created_at, updated_at, created_by, updated_by)
--     3. account_roles (account_id, role_id)
--     4. account_profiles (id, account_id [1-1], full_name, phone, address)
--     5. audit_logs (id, action, entity_type, entity_id, performed_by, old_value, new_value, detail, created_at)
--   Module SV2 (Catalog & Stock):
--     6. categories (id, name, description, parent_id [Self 1-N], active, created_at, updated_at)
--     7. items (id, sku, name, category_id [1-N], item_type [PHYSICAL/SERVICE], unit, price, weight, status, description, ...)
--     8. warehouses (id, code, name, location, active)
--     9. stock_items (id, warehouse_id [1-N], item_id, on_hand, reserved, low_stock_threshold, updated_at, updated_by)
--   Module SV3 (Order & Fulfillment):
--    10. carts (id, account_id, promotion_code, item_discount, cart_discount, updated_at)
--    11. cart_items (id, cart_id, item_id, quantity, unit_price)
--    12. orders (id, order_code, account_id, status, subtotal, item_discount, cart_discount, discount_amount, tax_amount, shipping_fee, grand_total, promotion_code, shipping_address, note, created_at, updated_at, created_by, updated_by)
--    13. order_items (id, order_id, item_id, quantity, unit_price, item_discount)
--    14. shipments (id, order_id [1-N], tracking_number, carrier, status, shipping_address, shipped_at, estimated_delivery, delivered_at, created_at)
--    15. appointments (id, order_id [1-N], service_name, appointment_time, location, staff_assigned, status, notes, created_at)
--   Module SV4 (Payment, Refund & Wallet):
--    16. payments (id, order_id, method, status, subtotal, item_discount, cart_discount, discount_amount, tax_amount, shipping_fee, cod_fee, gateway_fee, grand_total, wallet_used, amount_due, amount_paid, transaction_ref, paid_at, created_at)
--    17. refunds (id, order_id, payment_id, amount, refund_type [FULL/PARTIAL], reason, status, requested_by, processed_by, created_at, processed_at)
--    18. wallets (id, account_id, balance, total_deposited, total_spent, updated_at)
--    19. wallet_transactions (id, wallet_id, tx_type [DEPOSIT/PAYMENT/REFUND], amount, description, created_at)
--   Module SV5 (Promotion & Analytics):
--    20. promotions (id, code, scope [ITEM/CART], target_item_id, discount_type [PERCENTAGE/FIXED], discount_value, min_order_amount, max_usage, used_count, start_date, end_date, active, description)
--    21. promotion_usages (id, promotion_id [1-N], order_id, account_id, discount_amount, used_at)
-- =====================================================================
