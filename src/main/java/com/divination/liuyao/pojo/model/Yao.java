package com.divination.liuyao.pojo.model;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.LiuQin;
import com.divination.liuyao.assemblies.enums.LiuShen;
import com.divination.liuyao.assemblies.enums.ShiOrYing;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个爻的模型
 */
@Data
@NoArgsConstructor
public class Yao {
    /**
     * 爻的值：0-老阴，1-少阳，2-少阴，3-老阳
     */
    private Integer value;

    /**
     * 爻的位置（1-6，从下到上）
     */
    private Integer position;

    /**
     * 六亲
     */
    private LiuQin liuQin;

    /**
     * 对应的天干
     */
    private TianGan tianGan;

    /**
     * 对应的地支
     */
    private DiZhi diZhi;

    /**
     * 六神
     */
    private LiuShen liuShen;

    /**
     * 当前爻下藏伏的爻的信息
     */
    private String fuCang;

    /**
     * 世爻或者应爻
     */
    private ShiOrYing shiOrYing;

    /**
     * 是否是动爻
     */
    private Boolean isChange;


    public Yao(int value, int position) {
        this.value = value;
        this.position = position;
    }

    public Yao(int value, int position, TianGan tianGan, DiZhi diZhi, LiuQin liuQin, ShiOrYing shiOrYing, String fuCang, LiuShen liuShen) {
        this.value = value;
        this.position = position;
        this.liuQin = liuQin;
        this.tianGan = tianGan;
        this.diZhi = diZhi;
        this.liuShen = liuShen;
        this.fuCang = fuCang;
        this.shiOrYing = shiOrYing;
    }

    public boolean isNull(){
        return value == null || position == null || diZhi == null;
    }

    /**
     * 是否为动爻
     */
    public boolean isMoving() {
        return value == 0 || value == 3;  // 老阴或老阳为动爻
    }
    
    /**
     * 是否为阳爻
     */
    public boolean isYang() {
        return value == 1 || value == 3;  // 少阳或老阳为阳爻
    }

    /**
     * 是否为世爻
     */
    public boolean isShiYao() {
        return ShiOrYing.SHI.equals(shiOrYing) ;  // 少阳或老阳为阳爻
    }


    /**
     * 获取变化后的爻
     * 使用@JsonIgnore防止序列化时无限递归
     */
    @JsonIgnore
    public Yao getChangedYao() {
        if (value == 0) {  // 老阴变少阳
            return new Yao(1, position);
        } else if (value == 3) {  // 老阳变少阴
            return new Yao(2, position);
        } else {
            return new Yao(value, position);  // 少阴少阳不变
        }
    }
    
    /**
     * 获取爻的符号表示
     */
    public String getSymbol() {
        if (isYang()) {
            return "—";  // 阳爻
        } else {
            return "--";  // 阴爻
        }
    }
} 