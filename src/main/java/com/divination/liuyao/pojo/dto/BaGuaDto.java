package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaGuaDto {

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
     * 自定义时间（如果时图像识别，则会出现这个时间）
     */
    private String customTime;

}
