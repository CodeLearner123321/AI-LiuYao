package com.divination.liuyao.pojo.vo;

import com.divination.liuyao.pojo.model.AiResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 任务查询结果VO
 */
@Data
public class TaskQueryVO {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 任务是否完成
     */
    private boolean completed;
    
    /**
     * 任务结果数据
     */
    private JsonNode data;
    
    /**
     * 错误信息
     */
    private String error;
} 