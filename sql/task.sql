CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：LIUYAO-六爻起卦分析',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务状态：PENDING-等待处理，PROCESSING-处理中，COMPLETED-完成，FAILED-失败',
  `pre_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '预扣费金额',
  `actual_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '实际扣费金额，任务完成后填写',
  `is_charged` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已扣费：0-未扣费，1-已扣费',
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
) ENGINE=InnoDB AUTO_INCREMENT=1195 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='异步任务表'
;