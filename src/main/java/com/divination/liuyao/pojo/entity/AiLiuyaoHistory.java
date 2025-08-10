package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI六爻算卦历史记录实体
 * 记录用户成功使用AI算卦并获得结果的历史
 */
@Data
@TableName("ai_liuyao_history")
public class AiLiuyaoHistory {
    
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 历史记录ID，一个历史记录下可以会有多次对话，一个历史记录下的所有记录的HistoryId都相同
     */
    private Long historyId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 关联的任务ID
     */
    private Long taskId;


    /**
     * 用户提问的问题
     */
    private String question;

    /**
     * 问题背景
     */
    private String background;

    /**
     * 创建的类型：
     * 根据时间戳创建(系统时间起卦) 或者 根据数字创建(手动起卦 和 系统随机起卦)
     */
    private CastType castType;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 数字
     * 一共六位数字，对应分别为：0-老阴，1-少阳，2-少阴，3-老阳
     */
    private String number;

    /**
     * 用户选定的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime castTime;


    /**
     * 判词：确定用户所测结果的一行小诗
     */
    private String keyOutcome;

    /**
     * 卦象结果数据（JSON格式）
     */
    private String resultData;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 消费金额
     */
    private String amount;


    /**
     * 转换成BaGuaDto
     */
    public BaGuaDto convertBaGuaDto(){
        BaGuaDto baGuaDto = new BaGuaDto();
        baGuaDto.setCastType(this.castType);
        baGuaDto.setTimestamp(this.timestamp);
        baGuaDto.setNumber(this.number);
        baGuaDto.setCastTime(this.castTime);
        return baGuaDto;
    }
}