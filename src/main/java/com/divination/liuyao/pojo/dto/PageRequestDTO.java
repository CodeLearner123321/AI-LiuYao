package com.divination.liuyao.pojo.dto;

import lombok.Data;

/**
 * 分页请求参数数据传输对象
 */
@Data
public class PageRequestDTO {
    /**
     * 当前页码，默认为1
     */
    private Integer current = 1;
    
    /**
     * 每页大小，默认为10
     */
    private Integer size = 10;
}