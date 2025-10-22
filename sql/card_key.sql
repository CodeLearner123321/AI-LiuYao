-- 卡密表
CREATE TABLE IF NOT EXISTS `card_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '卡密ID',
    `card_code` VARCHAR(64) NOT NULL COMMENT '卡密码，唯一标识',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '卡密金额',
    `creator_id` BIGINT NOT NULL COMMENT '生成该卡密的用户ID',
    `user_id` BIGINT NULL COMMENT '使用该卡密的用户ID，未使用时为NULL',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '卡密状态：0-未使用，1-已使用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `use_time` DATETIME NULL COMMENT '使用时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_card_code` (`card_code`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡密表';

