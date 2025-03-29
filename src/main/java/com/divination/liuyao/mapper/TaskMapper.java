package com.divination.liuyao.mapper;

import com.divination.liuyao.pojo.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface TaskMapper {
    
    /**
     * 插入新任务
     * @param task 任务对象
     * @return 影响行数
     */
    int insert(Task task);
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     * @param errorMsg 错误信息，如果有的话
     * @return 影响行数
     */
    int updateStatus(@Param("taskId") Long taskId, 
                     @Param("status") String status, 
                     @Param("errorMsg") String errorMsg);

    
    /**
     * 更新扣费状态
     * @param taskId 任务ID
     * @param isCharged 扣费状态
     * @return 影响行数
     */
    int updateChargeStatus(@Param("taskId") Long taskId, 
                          @Param("isCharged") Integer isCharged);
    
    /**
     * 按ID查找任务
     * @param id 任务ID
     * @return 任务对象可选包装
     */
    Optional<Task> findById(Long id);
    
    /**
     * 查找用户最新的任务
     * @param userId 用户ID
     * @param taskType 任务类型
     * @return 任务对象可选包装
     */
    Optional<Task> findLatestByUserIdAndType(@Param("userId") Long userId, 
                                            @Param("taskType") String taskType);
    
    /**
     * 更新任务结果和状态
     * @param taskId 任务ID
     * @param resultData 结果数据JSON
     * @param status 新状态
     * @param errorMsg 错误信息，如果有的话
     * @return 影响行数
     */
    int updateResultAndStatus(@Param("taskId") Long taskId, 
                              @Param("resultData") String resultData,
                              @Param("status") String status, 
                              @Param("errorMsg") String errorMsg);

    /**
     * 更新任务结果、状态、实际扣费金额和扣费状态
     * @param taskId 任务ID
     * @param resultData 结果数据JSON
     * @param status 新状态
     * @param errorMsg 错误信息
     * @param actualAmount 实际扣费金额
     * @param isCharged 是否已扣费
     * @return 影响行数
     */
    int updateTaskComplete(
        @Param("taskId") Long taskId, 
        @Param("resultData") String resultData,
        @Param("status") String status, 
        @Param("errorMsg") String errorMsg,
        @Param("actualAmount") BigDecimal actualAmount,
        @Param("isCharged") Integer isCharged
    );
} 