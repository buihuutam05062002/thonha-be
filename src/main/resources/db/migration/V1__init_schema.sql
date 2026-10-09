-- =====================================================================
-- VuaTho - schema thống nhất (sinh từ các entity JPA trong src/main/java/.../entity)
-- Dùng với spring.jpa.hibernate.ddl-auto=validate: bảng/cột phải khớp entity.
-- Khi sửa entity, thêm file V<n+1>__...sql mới (không sửa file này sau khi đã chạy).
-- =====================================================================

CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `full_name` VARCHAR(100) NOT NULL,
    `username` VARCHAR(100),
    `email` VARCHAR(150),
    `phone_number` VARCHAR(20),
    `password` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `avatar_url` VARCHAR(500),
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    `lat` DECIMAL(10,7),
    `lng` DECIMAL(10,7),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_email` UNIQUE (`email`),
    CONSTRAINT `uk_users_phone_number` UNIQUE (`phone_number`),
    CONSTRAINT `uk_users_username` UNIQUE (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bank_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `bank_name` VARCHAR(100) NOT NULL,
    `account_number` VARCHAR(30) NOT NULL,
    `account_holder` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `worker_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `residence_city` VARCHAR(100) NOT NULL,
    `service_area` VARCHAR(255) NOT NULL,
    `years_of_experience` INT NOT NULL,
    `approval_status` VARCHAR(20) NOT NULL,
    `availability_status` VARCHAR(20) NOT NULL,
    `average_rating` DECIMAL(3,2) NOT NULL,
    `acceptance_rate` DECIMAL(5,2) NOT NULL,
    `active_job_count` INT NOT NULL,
    `created_at` DATETIME,
    `reviewed_at` DATETIME,
    `reviewed_by` BIGINT NULL,
    `reject_reason` VARCHAR(500),
    `bank_account_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_worker_profile_bank_account_id` UNIQUE (`bank_account_id`),
    CONSTRAINT `uk_worker_profile_user_id` UNIQUE (`user_id`),
    CONSTRAINT `fk_worker_profile_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_worker_profile_reviewed_by` FOREIGN KEY (`reviewed_by`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_worker_profile_bank_account_id` FOREIGN KEY (`bank_account_id`) REFERENCES `bank_account`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `wallet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `worker_id` BIGINT NOT NULL,
    `balance` DECIMAL(15,2) NOT NULL,
    `updated_at` DATETIME,
    `version` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_wallet_worker` UNIQUE (`worker_id`),
    CONSTRAINT `fk_wallet_worker_id` FOREIGN KEY (`worker_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `service_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `category_name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `icon` VARCHAR(255),
    `status` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_service_category_category_name` UNIQUE (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `label` VARCHAR(100),
    `full_address` VARCHAR(500) NOT NULL,
    `lat` DECIMAL(10,7),
    `lng` DECIMAL(10,7),
    `is_default` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_address_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `repair_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_code` VARCHAR(50) NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `address_id` BIGINT NULL,
    `worker_id` BIGINT NULL,
    `description` TEXT NOT NULL,
    `priority_level` VARCHAR(20) NOT NULL,
    `address_text` VARCHAR(500),
    `lat` DECIMAL(10,7),
    `lng` DECIMAL(10,7),
    `status` VARCHAR(30) NOT NULL,
    `final_price` DECIMAL(15,2),
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_repair_request_request_code` UNIQUE (`request_code`),
    CONSTRAINT `fk_repair_request_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_repair_request_category_id` FOREIGN KEY (`category_id`) REFERENCES `service_category`(`id`),
    CONSTRAINT `fk_repair_request_address_id` FOREIGN KEY (`address_id`) REFERENCES `address`(`id`),
    CONSTRAINT `fk_repair_request_worker_id` FOREIGN KEY (`worker_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `complainant_id` BIGINT NOT NULL,
    `handled_by_id` BIGINT NULL,
    `reason` VARCHAR(30) NOT NULL,
    `description` TEXT,
    `status` VARCHAR(20) NOT NULL,
    `resolution` TEXT,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_complaint_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_complaint_complainant_id` FOREIGN KEY (`complainant_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_complaint_handled_by_id` FOREIGN KEY (`handled_by_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(30) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_roles_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `worker_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `worker_profile_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_worker_document_worker_profile_id` FOREIGN KEY (`worker_profile_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `is_read` BOOLEAN NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_notification_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `promotion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL,
    `discount_type` VARCHAR(20) NOT NULL,
    `discount_value` DECIMAL(15,2) NOT NULL,
    `quantity` INT NOT NULL,
    `used_quantity` INT NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_promotion_code` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payment_transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `promotion_id` BIGINT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `payment_method` VARCHAR(20) NOT NULL,
    `commission_rate` DECIMAL(5,2),
    `commission_amount` DECIMAL(15,2),
    `worker_amount` DECIMAL(15,2),
    `status` VARCHAR(20) NOT NULL,
    `gateway_transaction_code` VARCHAR(100),
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_payment_transaction_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_payment_transaction_promotion_id` FOREIGN KEY (`promotion_id`) REFERENCES `promotion`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT,
    `message_type` VARCHAR(20) NOT NULL,
    `is_seen` BOOLEAN NOT NULL,
    `sent_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_message_request` (`request_id`, `id`),
    CONSTRAINT `fk_message_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_message_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `message_id` BIGINT NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_message_image_message_id` FOREIGN KEY (`message_id`) REFERENCES `message`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `worker_id` BIGINT NOT NULL,
    `rating` INT NOT NULL,
    `comment` TEXT,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_review_request` UNIQUE (`request_id`),
    CONSTRAINT `fk_review_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_review_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_review_worker_id` FOREIGN KEY (`worker_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `worker_specialty` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `worker_profile_id` BIGINT NOT NULL,
    `service_category_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_worker_specialty` UNIQUE (`worker_profile_id`, `service_category_id`),
    CONSTRAINT `fk_worker_specialty_worker_profile_id` FOREIGN KEY (`worker_profile_id`) REFERENCES `worker_profile`(`id`),
    CONSTRAINT `fk_worker_specialty_service_category_id` FOREIGN KEY (`service_category_id`) REFERENCES `service_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `matching_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `worker_id` BIGINT NOT NULL,
    `total_score` DECIMAL(7,4),
    `distance_score` DECIMAL(7,4),
    `rating_score` DECIMAL(7,4),
    `acceptance_rate_score` DECIMAL(7,4),
    `workload_score` DECIMAL(7,4),
    `result` VARCHAR(30) NOT NULL,
    `sent_at` DATETIME NOT NULL,
    `responded_at` DATETIME,
    `note` TEXT,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_matching_request_worker` UNIQUE (`request_id`, `worker_id`),
    CONSTRAINT `fk_matching_log_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_matching_log_worker_id` FOREIGN KEY (`worker_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(64) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `revoked_at` DATETIME,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_refresh_tokens_token_hash` UNIQUE (`token_hash`),
    CONSTRAINT `fk_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `complaint_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `complaint_id` BIGINT NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_complaint_image_complaint_id` FOREIGN KEY (`complaint_id`) REFERENCES `complaint`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `worker_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `worker_profile_id` BIGINT NOT NULL,
    `work_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_worker_schedule_worker_profile_id` FOREIGN KEY (`worker_profile_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `request_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `sort_order` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_request_attachment_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `withdrawal_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `wallet_id` BIGINT NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `created_at` DATETIME NOT NULL,
    `processed_at` DATETIME,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_withdrawal_request_wallet_id` FOREIGN KEY (`wallet_id`) REFERENCES `wallet`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `quotation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT NOT NULL,
    `worker_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(15,2) NOT NULL,
    `note` TEXT,
    `status` VARCHAR(20) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_quotation_request_id` FOREIGN KEY (`request_id`) REFERENCES `repair_request`(`id`),
    CONSTRAINT `fk_quotation_worker_id` FOREIGN KEY (`worker_id`) REFERENCES `worker_profile`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` INT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_user_roles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_user_roles_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
