CREATE TABLE `ai_liuyao_history` (
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
	`key_outcome` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '判词：确定用户所测结果的一行小诗',
	`result_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '卦象结果数据（JSON格式）',
	`duration_seconds` int NULL COMMENT '耗时秒数（单位：秒）',
	`is_accurate` tinyint(1) NULL COMMENT '是否算得准确：0-不准确，1-准确',
	`amount` decimal(10,2)  NOT NULL COMMENT '消费金额',
	`create_time` datetime NOT NULL COMMENT '创建时间',
	PRIMARY KEY (`id`),
	KEY `idx_user_id`(`user_id`) USING BTREE,
	KEY `idx_create_time`(`create_time`) USING BTREE,
	KEY `idx_task_id`(`task_id`) USING BTREE,
	KEY `idx_cast_type`(`cast_type`) USING BTREE,
	KEY `idx_history_id`(`history_id`) USING BTREE
) ENGINE=InnoDB
DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='AI六爻算卦历史记录表'
AUTO_INCREMENT=986
ROW_FORMAT=DYNAMIC
AVG_ROW_LENGTH=4232;
