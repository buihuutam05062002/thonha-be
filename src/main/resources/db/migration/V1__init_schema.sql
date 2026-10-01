-- ================================================================
-- VUA THỢ - DATABASE CHUNG
-- Customer / Worker / Admin
--
-- MySQL: localhost:3306
-- Database: vuatho
--
-- LƯU Ý:
-- 1. Script này XÓA database vuatho hiện tại và tạo lại từ đầu.
-- 2. Không dùng Flyway: có thể chạy script này trực tiếp bằng
--    MySQL Workbench, sau đó để Spring Boot sử dụng database này.
-- 3. repair_request.status dùng VARCHAR để Java Enum quản lý trạng thái.
-- ================================================================

DROP DATABASE IF EXISTS vuatho;

CREATE DATABASE vuatho
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vuatho;


-- ================================================================
-- 1. USERS & AUTHENTICATION
-- ================================================================

CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       full_name VARCHAR(100) NOT NULL,
                       username VARCHAR(100) NULL,
                       email VARCHAR(150) NULL,
                       phone_number VARCHAR(20) NULL,
                       password VARCHAR(255) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       avatar_url VARCHAR(500) NULL,
                       created_at DATETIME NOT NULL,
                       updated_at DATETIME NOT NULL,

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_email (email),
                       UNIQUE KEY uk_users_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE roles (
                       id INT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(30) NOT NULL,

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id INT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE refresh_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                expires_at DATETIME NOT NULL,
                                revoked_at DATETIME NULL,

                                PRIMARY KEY (id),
                                UNIQUE KEY uk_refresh_tokens_hash (token_hash),

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 2. CUSTOMER - ADDRESS
-- ================================================================

CREATE TABLE address (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         label VARCHAR(100) NULL,
                         full_address VARCHAR(500) NOT NULL,
                         lat DECIMAL(10,7) NULL,
                         lng DECIMAL(10,7) NULL,
                         is_default BOOLEAN NOT NULL DEFAULT FALSE,

                         PRIMARY KEY (id),

                         CONSTRAINT fk_address_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 3. SERVICE CATEGORY
-- ================================================================

CREATE TABLE service_category (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  name VARCHAR(100) NOT NULL,
                                  description VARCHAR(500) NULL,
                                  icon VARCHAR(255) NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_service_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 4. WORKER
-- ================================================================

CREATE TABLE worker_profile (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                province_city VARCHAR(100) NOT NULL,
                                operating_area VARCHAR(255) NOT NULL,
                                experience_years INT NOT NULL,
                                approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                availability_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
                                average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,
                                acceptance_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00,
                                ongoing_jobs_count INT NOT NULL DEFAULT 0,
                                reviewed_by BIGINT NULL,
                                reviewed_at DATETIME NULL,
                                reject_reason VARCHAR(500) NULL,

                                PRIMARY KEY (id),
                                UNIQUE KEY uk_worker_profile_user (user_id),

                                CONSTRAINT fk_worker_profile_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id),

                                CONSTRAINT fk_worker_profile_reviewer
                                    FOREIGN KEY (reviewed_by)
                                        REFERENCES users(id)
                                        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE worker_specialty (
                                  worker_profile_id BIGINT NOT NULL,
                                  service_category_id BIGINT NOT NULL,

                                  PRIMARY KEY (worker_profile_id, service_category_id),

                                  CONSTRAINT fk_worker_specialty_worker
                                      FOREIGN KEY (worker_profile_id)
                                          REFERENCES worker_profile(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_worker_specialty_category
                                      FOREIGN KEY (service_category_id)
                                          REFERENCES service_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE worker_document (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 worker_profile_id BIGINT NOT NULL,
                                 type VARCHAR(30) NOT NULL,
                                 file_url VARCHAR(500) NOT NULL,
                                 created_at DATETIME NOT NULL,

                                 PRIMARY KEY (id),

                                 CONSTRAINT fk_worker_document_worker
                                     FOREIGN KEY (worker_profile_id)
                                         REFERENCES worker_profile(id)
                                         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE worker_schedule (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 worker_profile_id BIGINT NOT NULL,
                                 apply_type VARCHAR(20) NOT NULL,
                                 specific_date DATE NULL,
                                 day_of_week TINYINT NULL,
                                 start_time TIME NOT NULL,
                                 end_time TIME NOT NULL,
                                 status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

                                 PRIMARY KEY (id),

                                 CONSTRAINT fk_worker_schedule_worker
                                     FOREIGN KEY (worker_profile_id)
                                         REFERENCES worker_profile(id)
                                         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 5. REPAIR REQUEST
-- ================================================================
--
-- status dùng VARCHAR thay vì MySQL ENUM.
-- Java RepairRequest.RepairStatus sẽ quản lý các giá trị:
--
-- PENDING      = Chờ ghép thợ
-- MATCHING     = Đang tìm/ghép thợ
-- ASSIGNED     = Đã ghép thợ
-- ON_THE_WAY   = Đang di chuyển
-- IN_PROGRESS  = Đang sửa
-- COMPLETED    = Hoàn thành
-- CANCELLED    = Đã hủy
--
-- Vì status là VARCHAR nên thêm ON_THE_WAY KHÔNG cần ALTER TABLE.
-- Chỉ cần thêm ON_THE_WAY vào Java Enum và frontend mapping.
-- ================================================================

CREATE TABLE repair_request (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                request_code VARCHAR(50) NOT NULL,
                                customer_id BIGINT NOT NULL,
                                category_id BIGINT NOT NULL,
                                address_id BIGINT NULL,
                                worker_id BIGINT NULL,
                                description TEXT NOT NULL,
                                priority_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                                address_text VARCHAR(500) NULL,
                                lat DECIMAL(10,7) NULL,
                                lng DECIMAL(10,7) NULL,

                                status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                final_price DECIMAL(15,2) NULL,
                                created_at DATETIME NOT NULL,
                                updated_at DATETIME NOT NULL,

                                PRIMARY KEY (id),
                                UNIQUE KEY uk_repair_request_code (request_code),

                                CONSTRAINT fk_repair_request_customer
                                    FOREIGN KEY (customer_id)
                                        REFERENCES users(id),

                                CONSTRAINT fk_repair_request_category
                                    FOREIGN KEY (category_id)
                                        REFERENCES service_category(id),

                                CONSTRAINT fk_repair_request_address
                                    FOREIGN KEY (address_id)
                                        REFERENCES address(id)
                                        ON DELETE SET NULL,

                                CONSTRAINT fk_repair_request_worker
                                    FOREIGN KEY (worker_id)
                                        REFERENCES worker_profile(id)
                                        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 6. REQUEST ATTACHMENTS
-- ================================================================

CREATE TABLE request_attachment (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    request_id BIGINT NOT NULL,
                                    type VARCHAR(20) NOT NULL,
                                    url VARCHAR(500) NOT NULL,
                                    sort_order INT NOT NULL DEFAULT 0,

                                    PRIMARY KEY (id),

                                    CONSTRAINT fk_request_attachment_request
                                        FOREIGN KEY (request_id)
                                            REFERENCES repair_request(id)
                                            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 7. MATCHING
-- ================================================================

CREATE TABLE matching_log (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              request_id BIGINT NOT NULL,
                              worker_id BIGINT NOT NULL,
                              score DECIMAL(8,2) NULL,
                              result VARCHAR(20) DEFAULT 'PENDING',
                              sent_at DATETIME NULL,
                              response_at DATETIME NULL,
                              note TEXT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT fk_matching_log_request
                                  FOREIGN KEY (request_id)
                                      REFERENCES repair_request(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_matching_log_worker
                                  FOREIGN KEY (worker_id)
                                      REFERENCES worker_profile(id)
                                      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 8. WALLET & WITHDRAWAL
-- ================================================================

CREATE TABLE wallet (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        worker_profile_id BIGINT NOT NULL,
                        balance DECIMAL(15,2) NOT NULL DEFAULT 0,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        PRIMARY KEY (id),
                        UNIQUE KEY uk_wallet_worker (worker_profile_id),

                        CONSTRAINT fk_wallet_worker
                            FOREIGN KEY (worker_profile_id)
                                REFERENCES worker_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE withdrawal_request (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    wallet_id BIGINT NOT NULL,
                                    amount DECIMAL(15,2) NOT NULL,
                                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                    created_at DATETIME NOT NULL,
                                    processed_at DATETIME NULL,

                                    PRIMARY KEY (id),

                                    CONSTRAINT fk_withdrawal_wallet
                                        FOREIGN KEY (wallet_id)
                                            REFERENCES wallet(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 9. QUOTATION
-- ================================================================

CREATE TABLE quotation (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           request_id BIGINT NOT NULL,
                           worker_id BIGINT NOT NULL,
                           total_amount DECIMAL(15,2) NOT NULL,
                           notes TEXT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                           created_at DATETIME NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT fk_quotation_request
                               FOREIGN KEY (request_id)
                                   REFERENCES repair_request(id),

                           CONSTRAINT fk_quotation_worker
                               FOREIGN KEY (worker_id)
                                   REFERENCES worker_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 10. PAYMENT / TRANSACTION
-- ================================================================

CREATE TABLE transaction_record (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    request_id BIGINT NOT NULL,
                                    amount DECIMAL(15,2) NOT NULL,
                                    payment_method VARCHAR(30) NOT NULL,
                                    commission_rate DECIMAL(5,2) NOT NULL,
                                    commission_amount DECIMAL(15,2) NOT NULL,
                                    worker_earning DECIMAL(15,2) NOT NULL,
                                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                    created_at DATETIME NOT NULL,

                                    PRIMARY KEY (id),

                                    CONSTRAINT fk_transaction_request
                                        FOREIGN KEY (request_id)
                                            REFERENCES repair_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 11. REVIEW
-- ================================================================

CREATE TABLE review (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        request_id BIGINT NOT NULL,
                        customer_id BIGINT NOT NULL,
                        worker_id BIGINT NOT NULL,
                        star_rating TINYINT NOT NULL,
                        comment TEXT NULL,
                        created_at DATETIME NOT NULL,

                        PRIMARY KEY (id),

                        CONSTRAINT fk_review_request
                            FOREIGN KEY (request_id)
                                REFERENCES repair_request(id),

                        CONSTRAINT fk_review_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES users(id),

                        CONSTRAINT fk_review_worker
                            FOREIGN KEY (worker_id)
                                REFERENCES worker_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 12. COMPLAINT
-- ================================================================

CREATE TABLE complaint (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           request_id BIGINT NOT NULL,
                           handling_admin_id BIGINT NULL,
                           reason VARCHAR(30) NOT NULL,
                           description TEXT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                           resolution TEXT NULL,
                           created_at DATETIME NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT fk_complaint_request
                               FOREIGN KEY (request_id)
                                   REFERENCES repair_request(id),

                           CONSTRAINT fk_complaint_admin
                               FOREIGN KEY (handling_admin_id)
                                   REFERENCES users(id)
                                   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 13. NOTIFICATION
-- ================================================================

CREATE TABLE notification (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              type VARCHAR(30) NOT NULL,
                              content VARCHAR(500) NOT NULL,
                              is_read BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at DATETIME NOT NULL,

                              PRIMARY KEY (id),

                              CONSTRAINT fk_notification_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 14. MESSAGE
-- ================================================================

CREATE TABLE message (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         request_id BIGINT NOT NULL,
                         sender_id BIGINT NOT NULL,
                         content TEXT NOT NULL,
                         is_seen BOOLEAN NOT NULL DEFAULT FALSE,
                         sent_at DATETIME NOT NULL,

                         PRIMARY KEY (id),

                         CONSTRAINT fk_message_request
                             FOREIGN KEY (request_id)
                                 REFERENCES repair_request(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT fk_message_sender
                             FOREIGN KEY (sender_id)
                                 REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 15. PROMOTION
-- ================================================================

CREATE TABLE promotion (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           promo_code VARCHAR(50) NOT NULL,
                           discount_type VARCHAR(20) NOT NULL,
                           value DECIMAL(15,2) NOT NULL,
                           quantity INT NOT NULL DEFAULT 0,
                           used_quantity INT NOT NULL DEFAULT 0,
                           start_date DATE NOT NULL,
                           end_date DATE NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                           PRIMARY KEY (id),
                           UNIQUE KEY uk_promotion_code (promo_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 16. DỮ LIỆU MẶC ĐỊNH
-- ================================================================

INSERT INTO roles (name)
VALUES
    ('ADMIN'),
    ('CUSTOMER'),
    ('WORKER');


INSERT INTO service_category
(name, description, icon, status)
VALUES
    (
        'Điện dân dụng',
        'Ổ cắm, công tắc, dây điện, aptomat',
        'zap',
        'ACTIVE'
    ),
    (
        'Nước & đường ống',
        'Vòi nước, đường ống, bồn rửa và rò rỉ',
        'droplet',
        'ACTIVE'
    ),
    (
        'Điều hòa',
        'Không lạnh, chảy nước, vệ sinh và nạp gas',
        'wind',
        'ACTIVE'
    ),
    (
        'Tủ lạnh',
        'Không lạnh, đóng tuyết và tiếng ồn',
        'refrigerator',
        'ACTIVE'
    ),
    (
        'Máy giặt',
        'Không vắt, không cấp nước và rò nước',
        'washing-machine',
        'ACTIVE'
    ),
    (
        'Khác',
        'Các vấn đề sửa chữa thiết bị gia dụng khác',
        'wrench',
        'ACTIVE'
    );


-- ================================================================
-- 17. KIỂM TRA DATABASE
-- ================================================================

SELECT id, name, description, icon, status
FROM service_category
ORDER BY id;

SELECT id, status, request_code, customer_id, worker_id
FROM repair_request
ORDER BY id DESC;
