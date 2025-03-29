package com.divination.liuyao.pojo.model;

import com.divination.liuyao.util.ConstantUtil;

public class AiResult {

    /**
     * 判辞
     */
    private String keyOutcome;

    /**
     * 返回的文本内容
     */
    private String text;

    // Getter and Setter methods
    
    public String getKeyOutcome() {
        return keyOutcome;
    }
    
    public void setKeyOutcome(String keyOutcome) {
        this.keyOutcome = keyOutcome;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }

    public Boolean isFalse(){
        return ConstantUtil.AI_ERROR_RESULT_KEY.equals(keyOutcome);
    }
}
