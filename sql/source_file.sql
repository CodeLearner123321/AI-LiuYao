CREATE TABLE `source_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `oss_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'OSS存储地址',
  `file_md5` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件MD5用于去重',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'pdf/docx/txt等',
  `full_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析后的完整文本内容',
  `text_length` int DEFAULT NULL COMMENT '文本字符长度',
  `total_cases` int DEFAULT '0' COMMENT '识别出的卦例总数',
  `uploaded_by` bigint NOT NULL COMMENT '上传用户ID',
  `uploader_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'user/admin',
  `parse_status` tinyint DEFAULT '0' COMMENT '0未解析 1已解析 2失败',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '软删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_md5` (`file_md5`) USING BTREE,
  KEY `idx_uploaded_by` (`uploaded_by`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='原始文件表（卦例检测来源）'
;
