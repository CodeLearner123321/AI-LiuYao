package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * AI六爻历史记录数据访问层接口
 * 继承BaseMapper获得通用的CRUD方法
 */
@Mapper
public interface AiLiuyaoHistoryMapper extends BaseMapper<AiLiuyaoHistory> {
    
    /**
     * 根据ID查询历史记录
     * @param id 记录ID
     * @return 历史记录对象的可选包装
     */
    Optional<AiLiuyaoHistory> findById(Long id);
    
    /**
     * 根据用户ID查询问题列表
     */
    List<AiLiuyaoHistory> findQuestionListByUserId(Long userId);

    /**
     * 根据任务ID查询历史记录
     * @param taskId 任务ID
     * @return 历史记录对象的可选包装
     */
    Optional<AiLiuyaoHistory> findByTaskId(Long taskId);
    
    /**
     * 更新历史记录的准确性反馈
     * @param id 记录ID
     * @param isAccurate 准确性反馈：0-不准确，1-准确
     * @return 更新影响的行数
     */
    int updateIsAccurate(@Param("id") Long id, @Param("isAccurate") Integer isAccurate);
} 