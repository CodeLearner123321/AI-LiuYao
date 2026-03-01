package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.liuyao.pojo.entity.SourceFile;
import com.divination.liuyao.pojo.vo.SourceFileStatusVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * source_file 表 Mapper。
 * 常规 CRUD 由 MyBatis-Plus BaseMapper 提供；自定义 SQL 见 SourceFileMapper.xml。
 */
@Mapper
public interface SourceFileMapper extends BaseMapper<SourceFile> {

    /**
     * 按需查询文件解析状态（仅返回前端轮询所需字段，不含 full_text、oss_url 等）。
     *
     * @param id source_file 主键
     * @return 状态 VO，不存在则返回 null
     */
    SourceFileStatusVO selectStatusById(@Param("id") Long id);

    /**
     * 按 MD5 查找文件记录（用于上传去重）。
     *
     * @param md5 文件 MD5（32 位十六进制）
     * @return 已存在的记录，不存在则返回 null
     */
    SourceFile findByMd5(@Param("md5") String md5);

    /**
     * 更新解析状态和卦例总数（检测完成后调用）。
     *
     * @param id          source_file 主键
     * @param parseStatus 解析状态（1=成功 2=失败）
     * @param totalCases  识别到的卦例总数
     */
    int updateParseResult(@Param("id") Long id,
                         @Param("parseStatus") int parseStatus,
                         @Param("totalCases") int totalCases);
}
