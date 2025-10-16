CREATE TABLE `file_info` (
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文件信息表'
;