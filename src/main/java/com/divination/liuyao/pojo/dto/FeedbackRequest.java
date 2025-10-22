package com.divination.liuyao.pojo.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackRequest {

    private Long id;

    private Integer isAccurate;
}
