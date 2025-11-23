package com.divination.liuyao.pojo.vo;

import com.divination.liuyao.pojo.model.Hexagram;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecognizeImageVo {

    private Hexagram  hexagram;

    private BigDecimal price;
}
