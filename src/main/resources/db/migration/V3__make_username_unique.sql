UPDATE `users` u
JOIN (
    SELECT `username`, MIN(`id`) AS keeper_id
    FROM `users`
    GROUP BY `username`
    HAVING COUNT(*) > 1
) dupes ON u.`username` = dupes.`username` AND u.`id` <> dupes.keeper_id
SET u.`username` = CONCAT(u.`username`, '_', u.`id`);

ALTER TABLE `users` ADD CONSTRAINT `uk_users_username` UNIQUE (`username`);