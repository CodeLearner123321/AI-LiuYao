package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.liuyao.pojo.entity.HexagramCase;
import com.divination.liuyao.pojo.vo.HexagramCaseListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * hexagram_case 表 Mapper。
 * 自定义 SQL 见 HexagramCaseMapper.xml。
 */
@Mapper
public interface HexagramCaseMapper extends BaseMapper<HexagramCase> {

    /**
     * 按需查询指定文件下的卦例列表（仅返回展示所需字段，不含 raw_ai_json 等）。
     *
     * @param sourceFileId 原始文件 ID
     * @return 卦例列表 VO，按 case_index 升序
     */
    List<HexagramCaseListVO> selectListBySourceFileId(@Param("sourceFileId") Long sourceFileId);

    /**
     * 查询指定文件下的所有卦例（按序号升序，完整实体，供内部使用）。
     *
     * @param sourceFileId 原始文件 ID
     * @return 该文件下的所有卦例列表
     */
    List<HexagramCase> findBySourceFileId(@Param("sourceFileId") Long sourceFileId);
}
