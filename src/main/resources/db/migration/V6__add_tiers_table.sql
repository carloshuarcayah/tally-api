CREATE TABLE `tiers` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL,
    `max_categories` int NOT NULL,
    `max_budgets` int NOT NULL,
    `created_at` datetime(6) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK_tiers_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tiers` (`name`, `max_categories`, `max_budgets`, `created_at`, `updated_at`) VALUES
    ('FREE', 5, 4, NOW(6), NOW(6)),
    ('PREMIUM', 50, 30, NOW(6), NOW(6));

ALTER TABLE `users` ADD COLUMN `tier_id` bigint NULL;

UPDATE `users` SET `tier_id` = (SELECT `id` FROM `tiers` WHERE `name` = 'FREE');

ALTER TABLE `users` MODIFY COLUMN `tier_id` bigint NOT NULL;

ALTER TABLE `users` ADD CONSTRAINT `FK_users_tier` FOREIGN KEY (`tier_id`) REFERENCES `tiers` (`id`);
