-- =====================================================================
-- V2: Transform schema từ V1 (USERS.id) sang entity hiện tại (users.user_id)
-- Sử dụng ALTER TABLE ... ALGORITHM=INPLACE cho MySQL 8.0+
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. Drop FK constraints trước khi rename columns
-- ---------------------------------------------------------------------
ALTER TABLE `user_roles` DROP FOREIGN KEY `fk_user_role_user`;
ALTER TABLE `user_roles` DROP FOREIGN KEY `fk_user_role_role`;
ALTER TABLE `WORKER_PROFILE` DROP FOREIGN KEY `fk_worker_profile_user`;
ALTER TABLE `WORKER_PROFILE` DROP FOREIGN KEY `fk_worker_profile_bank`;
ALTER TABLE `WORKER_SPECIALTY` DROP FOREIGN KEY `fk_specialty_profile`;
ALTER TABLE `WORKER_SPECIALTY` DROP FOREIGN KEY `fk_specialty_category`;
ALTER TABLE `WORKER_DOCUMENT` DROP FOREIGN KEY `fk_document_profile`;
ALTER TABLE `worker_document` DROP FOREIGN KEY `fk_document_profile`;

-- ---------------------------------------------------------------------
-- 2. Transform USERS -> users
-- ---------------------------------------------------------------------
RENAME TABLE `USERS` TO `users`;

ALTER TABLE `users`
    CHANGE COLUMN `id` `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN `full_name` `fullName` VARCHAR(100) NOT NULL,
    CHANGE COLUMN `user` `username` VARCHAR(50) NULL,
    CHANGE COLUMN `email` `email` VARCHAR(100) NOT NULL,
    CHANGE COLUMN `phone_number` `phoneNumber` VARCHAR(15) NOT NULL,
    CHANGE COLUMN `password` `password` VARCHAR(255) NOT NULL,
    CHANGE COLUMN `status` `userStatus` ENUM('ACTIVE','INACTIVE','LOCKED','PENDING_VERIFY','DELETED') NOT NULL DEFAULT 'ACTIVE',
    CHANGE COLUMN `avatar_url` `avatar` VARCHAR(500) NULL,
    CHANGE COLUMN `created_at` `createdAt` DATETIME NOT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE `users`
    DROP INDEX `uk_users_email`,
    DROP INDEX `uk_users_phone_number`,
    ADD UNIQUE INDEX `uk_users_email` (`email`),
    ADD UNIQUE INDEX `uk_users_phone_number` (`phoneNumber`);

-- ---------------------------------------------------------------------
-- 3. Transform Role -> role
-- ---------------------------------------------------------------------
RENAME TABLE `Role` TO `role`;

ALTER TABLE `role`
    CHANGE COLUMN `id` `role_id` INT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN `name` `name` VARCHAR(50) NOT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

-- ---------------------------------------------------------------------
-- 4. Transform USER_ROLES -> user_roles
-- ---------------------------------------------------------------------
RENAME TABLE `USER_ROLES` TO `user_roles`;

ALTER TABLE `user_roles`
    CHANGE COLUMN `user_id` `user_id` BIGINT NOT NULL,
    CHANGE COLUMN `role_id` `role_id` INT NOT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE `user_roles`
    ADD CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- 5. Transform WORKER_PROFILE
-- ---------------------------------------------------------------------
RENAME TABLE `WORKER_PROFILE` TO `worker_profile`;

-- Add new columns
ALTER TABLE `worker_profile`
    ADD COLUMN `provinceCity` VARCHAR(100) NOT NULL DEFAULT '' AFTER `user_id`,
    ADD COLUMN `operatingArea` VARCHAR(255) NOT NULL DEFAULT '' AFTER `provinceCity`,
    ADD COLUMN `experienceYears` INT NOT NULL DEFAULT 0 AFTER `operatingArea`,
    ADD COLUMN `reviewed_by` BIGINT NULL AFTER `active_job_count`,
    ADD COLUMN `reviewed_at` DATETIME NULL AFTER `reviewed_by`,
    ADD COLUMN `reject_reason` VARCHAR(500) NULL AFTER `reviewed_at`,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Migrate data from old columns
UPDATE `worker_profile`
SET
    `provinceCity` = COALESCE(`residence_city`, ''),
    `operatingArea` = COALESCE(`service_area`, ''),
    `experienceYears` = COALESCE(`years_of_experience`, 0);

-- Add new enum columns
ALTER TABLE `worker_profile`
    ADD COLUMN `approvalStatus` ENUM('PENDING','APPROVED','REJECT') NOT NULL DEFAULT 'PENDING' AFTER `experienceYears`,
    ADD COLUMN `availabilityStatus` ENUM('READY','OFFLINE','BUSY') NOT NULL DEFAULT 'OFFLINE' AFTER `approvalStatus`,
    ADD COLUMN `averageRating` DECIMAL(3,2) NOT NULL DEFAULT 0.00 AFTER `availabilityStatus`,
    ADD COLUMN `acceptanceRate` DECIMAL(5,2) NOT NULL DEFAULT 100.00 AFTER `averageRating`,
    ADD COLUMN `ongoingJobsCount` INT NOT NULL DEFAULT 0 AFTER `acceptanceRate`,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Migrate enum data
UPDATE `worker_profile`
SET
    `approvalStatus` = CASE
        WHEN `approval_status` = 'pending' THEN 'PENDING'
        WHEN `approval_status` = 'approved' THEN 'APPROVED'
        WHEN `approval_status` = 'rejected' THEN 'REJECT'
        ELSE 'PENDING'
    END,
    `availabilityStatus` = CASE
        WHEN `availability_status` = 'online' THEN 'READY'
        WHEN `availability_status` = 'offline' THEN 'OFFLINE'
        WHEN `availability_status` = 'busy' THEN 'BUSY'
        ELSE 'OFFLINE'
    END,
    `averageRating` = COALESCE(`avg_rating`, 0),
    `acceptanceRate` = COALESCE(`acceptance_rate`, 100),
    `ongoingJobsCount` = COALESCE(`active_job_count`, 0);

-- Drop old columns
ALTER TABLE `worker_profile`
    DROP COLUMN `service_area`,
    DROP COLUMN `approval_status`,
    DROP COLUMN `availability_status`,
    DROP COLUMN `avg_rating`,
    DROP COLUMN `acceptance_rate`,
    DROP COLUMN `active_job_count`,
    DROP COLUMN `residence_city`,
    DROP COLUMN `years_of_experience`,
    DROP COLUMN `bank_account_id`,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Add FKs
ALTER TABLE `worker_profile`
    ADD CONSTRAINT `fk_worker_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `fk_worker_profile_reviewed_by` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`user_id`);

-- ---------------------------------------------------------------------
-- 6. Transform WORKER_DOCUMENT
-- ---------------------------------------------------------------------
RENAME TABLE `WORKER_DOCUMENT` TO `worker_document`;

-- Add new enum column
ALTER TABLE `worker_document`
    ADD COLUMN `type_new` ENUM('CCCD_FRONT','CCCD_BACK','CERTIFICATE','DEGREE') NOT NULL DEFAULT 'CCCD_FRONT' AFTER `worker_profile_id`,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Migrate data
UPDATE `worker_document`
SET `type_new` = CASE
    WHEN `type` = 'id_card' THEN 'CCCD_FRONT'
    WHEN `type` = 'certificate' THEN 'CERTIFICATE'
    ELSE 'CCCD_FRONT'
END;

-- Drop old, rename new
ALTER TABLE `worker_document`
    DROP COLUMN `type`,
    CHANGE COLUMN `type_new` `type` ENUM('CCCD_FRONT','CCCD_BACK','CERTIFICATE','DEGREE') NOT NULL,
    CHANGE COLUMN `url` `fileUrl` VARCHAR(500) NOT NULL,
    ADD COLUMN `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `fileUrl`,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Add FK
ALTER TABLE `worker_document`
    ADD CONSTRAINT `fk_worker_document_profile` FOREIGN KEY (`worker_profile_id`) REFERENCES `worker_profile` (`id`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------
-- 7. Transform SERVICE_CATEGORY
-- ---------------------------------------------------------------------
RENAME TABLE `SERVICE_CATEGORY` TO `service_category`;

ALTER TABLE `service_category`
    CHANGE COLUMN `id` `id` BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN `category_name` `name` VARCHAR(100) NOT NULL,
    CHANGE COLUMN `description` `description` VARCHAR(500) NULL,
    CHANGE COLUMN `icon` `icon` VARCHAR(255) NULL,
    CHANGE COLUMN `status` `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    ALGORITHM=INPLACE, LOCK=NONE;

UPDATE `service_category` SET `status` = UPPER(`status`);

-- ---------------------------------------------------------------------
-- 8. Transform WORKER_SPECIALTY
-- ---------------------------------------------------------------------
RENAME TABLE `WORKER_SPECIALTY` TO `worker_specialty`;

ALTER TABLE `worker_specialty`
    CHANGE COLUMN `id` `id` BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN `worker_profile_id` `worker_profile_id` BIGINT NOT NULL,
    CHANGE COLUMN `category_id` `service_category_id` BIGINT NOT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE `worker_specialty`
    ADD CONSTRAINT `fk_worker_specialty_profile` FOREIGN KEY (`worker_profile_id`) REFERENCES `worker_profile` (`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_worker_specialty_category` FOREIGN KEY (`service_category_id`) REFERENCES `service_category` (`id`);

-- ---------------------------------------------------------------------
-- 9. Drop tables không dùng
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ADDRESS`;
DROP TABLE IF EXISTS `PROMOTION`;
DROP TABLE IF EXISTS `BANK_ACCOUNT`;
DROP TABLE IF EXISTS `WORKER_SCHEDULE`;
DROP TABLE IF EXISTS `REPAIR_REQUEST`;
DROP TABLE IF EXISTS `REQUEST_ATTACHMENT`;
DROP TABLE IF EXISTS `QUOTATION`;
DROP TABLE IF EXISTS `MATCHING_LOG`;
DROP TABLE IF EXISTS `TRANSACTION`;
DROP TABLE IF EXISTS `REVIEW`;
DROP TABLE IF EXISTS `COMPLAINT`;
DROP TABLE IF EXISTS `COMPLAINT_IMAGE`;
DROP TABLE IF EXISTS `MESSAGE`;
DROP TABLE IF EXISTS `MESSAGE_IMAGE`;
DROP TABLE IF EXISTS `NOTIFICATION`;
DROP TABLE IF EXISTS `WALLET`;
DROP TABLE IF EXISTS `WITHDRAWAL_REQUEST`;

SET FOREIGN_KEY_CHECKS = 1;