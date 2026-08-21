CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱地址',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `pass_word` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `phone_number` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号(现在废弃)',
  `salt` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码盐值',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是VIP用户：0-否，1-是',
  `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账户余额，1元=10点',
  `frozen_balance` decimal(10,2) DEFAULT '0.00' COMMENT '冻结余额，用于订单支付过程中的中间状态',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_name` (`user_name`) USING BTREE,
  UNIQUE KEY `uk_phone_number` (`phone_number`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：LIUYAO-六爻起卦分析',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态：PENDING-等待处理，PROCESSING-处理中，COMPLETED-完成，FAILED-失败',
  `pre_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '预扣费金额',
  `actual_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '实际扣费金额，任务完成后填写',
  `is_charged` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已扣费：0-未扣费，1-已扣费',
  `payment_type` tinyint NOT NULL DEFAULT '2' COMMENT '支付类型：0-免费额度，1-余额，2-用户自定义API',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '请求参数JSON',
  `result_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '结果数据JSON',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='异步任务表';

CREATE TABLE IF NOT EXISTS `file_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uploader_user_id` bigint DEFAULT NULL COMMENT '上传者userId',
  `uploader_username` varchar(255) DEFAULT NULL COMMENT '上传者名称',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名称',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件格式',
  `image_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片名称',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '作者',
  `create_time` datetime DEFAULT NULL COMMENT '上传时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除0:未删除,1:已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_uploader_user_id` (`uploader_user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文件信息表';

CREATE TABLE IF NOT EXISTS `ai_liuyao_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `history_id` bigint NOT NULL COMMENT '历史记录ID，一个历史记录下可以会有多次对话',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_id` bigint NOT NULL COMMENT '关联的任务ID',
  `question` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户提问的问题',
  `background` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '问题背景',
  `cast_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '创建的类型：TIME-时间起卦，MANUAL-手动起卦，RANDOM-随机起卦',
  `timestamp` bigint NULL COMMENT '时间戳，当cast_type为TIME时有值',
  `number` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数字，一共六位数字，对应分别为：0-老阴，1-少阳，2-少阴，3-老阳',
  `cast_time` datetime NOT NULL COMMENT '用户选定的时间',
  `custom_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '自定义起卦时间描述',
  `key_outcome` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '判词：确定用户所测结果的一行小诗',
  `result_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '卦象结果数据（JSON格式）',
  `duration_seconds` int NULL COMMENT '耗时秒数（单位：秒）',
  `is_accurate` tinyint(1) NULL COMMENT '是否算得准确：0-不准确，1-准确',
  `amount` decimal(10,2) NOT NULL COMMENT '消费金额',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`(`user_id`) USING BTREE,
  KEY `idx_create_time`(`create_time`) USING BTREE,
  KEY `idx_task_id`(`task_id`) USING BTREE,
  KEY `idx_cast_type`(`cast_type`) USING BTREE,
  KEY `idx_history_id`(`history_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI六爻算卦历史记录表' ROW_FORMAT=DYNAMIC;

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
