CREATE TABLE `hexagram_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_file_id` bigint NOT NULL COMMENT '所属文件ID',
  `case_index` int NOT NULL COMMENT '该文件中的第几个卦例(从1开始)',
  `start_offset` int NOT NULL COMMENT '起始字符位置',
  `end_offset` int NOT NULL COMMENT '结束字符位置',
  `case_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '完整卦例文本(含背景与断语)',
  `ai_confidence` decimal(4,3) NOT NULL COMMENT '识别置信度',
  `hexagram_number` int DEFAULT '1' COMMENT '卦例数量',
  `raw_ai_json` json DEFAULT NULL COMMENT 'AI原始结构返回JSON',
  `ai_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '使用的模型名称',
  `structure_version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'v1' COMMENT '结构版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_file` (`source_file_id`) USING BTREE,
  KEY `idx_case_index` (`case_index`) USING BTREE,
  KEY `idx_confidence` (`ai_confidence`) USING BTREE,
  CONSTRAINT `fk_hexagram_case_source_file` FOREIGN KEY (`source_file_id`) REFERENCES `source_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='六爻卦例表'
;
