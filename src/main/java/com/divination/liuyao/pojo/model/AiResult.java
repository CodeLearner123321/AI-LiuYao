package com.divination.liuyao.pojo.model;

import com.divination.liuyao.util.ConstantUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiResult {

    /**
     * 判辞
     */
    private String keyOutcome;

    /**
     * 返回的文本内容
     */
    private String text;

    private Long inputToken;

    private Long outputToken;

    private Long imageToken;

    // Getter and Setter methods

    public Boolean isFalse(){
        return ConstantUtil.AI_ERROR_RESULT_KEY.equals(keyOutcome);
    }
}
