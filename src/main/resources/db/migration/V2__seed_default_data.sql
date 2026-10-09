-- Dữ liệu mặc định: vai trò và danh mục dịch vụ.
-- Tài khoản admin KHÔNG seed ở đây: DataInitializer tự tạo từ ADMIN_EMAIL / ADMIN_PASSWORD khi khởi động.

INSERT INTO `roles` (`name`) VALUES ('ADMIN'), ('CUSTOMER'), ('WORKER');

INSERT INTO `service_category` (`category_name`, `description`, `icon`, `status`) VALUES
    ('Điện dân dụng', 'Ổ cắm, công tắc, dây điện, aptomat', 'zap', 'ACTIVE'),
    ('Nước & đường ống', 'Vòi nước, đường ống, bồn rửa và rò rỉ', 'droplet', 'ACTIVE'),
    ('Điều hòa', 'Không lạnh, chảy nước, vệ sinh và nạp gas', 'wind', 'ACTIVE'),
    ('Tủ lạnh', 'Không lạnh, đóng tuyết và tiếng ồn', 'refrigerator', 'ACTIVE'),
    ('Máy giặt', 'Không vắt, không cấp nước và rò nước', 'washing-machine', 'ACTIVE'),
    ('Khác', 'Các vấn đề sửa chữa thiết bị gia dụng khác', 'wrench', 'ACTIVE');
