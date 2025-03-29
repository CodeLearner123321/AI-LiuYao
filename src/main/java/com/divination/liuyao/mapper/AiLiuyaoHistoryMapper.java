package com.divination.liuyao.mapper;

import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AiLiuyaoHistoryMapper {
    
    /**
     * 插入新的历史记录
     * @param history 历史记录对象
     * @return 影响行数
     */
    int insert(AiLiuyaoHistory history);
    
    /**
     * 根据ID查询历史记录
     * @param id 记录ID
     * @return 历史记录对象的可选包装
     */
    Optional<AiLiuyaoHistory> findById(Long id);
    
    List<AiLiuyaoHistory> findQuestionListByUserId(Long userId);

    /**
     * 根据任务ID查询历史记录
     * @param taskId 任务ID
     * @return 历史记录对象的可选包装
     */
    Optional<AiLiuyaoHistory> findByTaskId(Long taskId);
    
    /**
     * 更新历史记录
     * @param history 历史记录对象
     * @return 影响行数
     */
    int update(AiLiuyaoHistory history);
    
    /**
     * 删除历史记录
     * @param id 记录ID
     * @return 影响行数
     */
    int deleteById(Long id);
} 