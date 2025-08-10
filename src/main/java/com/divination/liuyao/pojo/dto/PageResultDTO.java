package com.divination.liuyao.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页结果数据传输对象
 * @param <T> 数据类型
 */
@Data
public class PageResultDTO<T> {
    /**
     * 当前页码
     */
    private long current;
    
    /**
     * 每页大小
     */
    private long size;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 总页数
     */
    private long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
}