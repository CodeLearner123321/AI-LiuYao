package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public enum DiZhi {
    ZI("子", "水","寅","子","酉","辰","申,酉","亥"),
    CHOU("丑", "土","亥","酉","午","丑","申,酉","子"),
    YIN("寅", "木","申","午","卯","戌","午,未","丑"),
    MAO("卯", "木","巳","卯","子","未","午,未","寅"),
    CHEN("辰", "土","寅","子","酉","辰","辰,巳","卯"),
    SI("巳", "火","亥","酉","午","丑","辰,巳","辰"),
    WU("午", "火","申","午","卯","戌","寅,卯","巳"),
    WEI("未", "土","巳","卯","子","未","寅,卯","午"),
    SHEN("申", "金","寅","子","酉","辰","子,丑","未"),
    YOU("酉", "金","亥","酉","午","丑","子,丑","申"),
    XU("戌", "土","申","午","卯","戌","戌,亥","酉"),
    HAI("亥", "水","巳","卯","子","未","戌,亥","戌");

    private final String name;

    private final String shuXin;

    //马星-以世爻所临地支确定
    private final String yiMa;

    //将星-以日支确定
    private final String jiangXin;

    //咸池-以世爻所临地支确定
    private final String xianChi;

    //华盖-以世爻所临地支确定
    private final String huaGai;

    //天喜
    private final String tianXi;

    //天医
    private final String tianYi;


    // 构造方法
    DiZhi(String name, String shuXin, String yiMa,String jiangXin, String xianChi, String huaGai, String tianXi, String tianYi) {
        this.name = name;
        this.shuXin = shuXin;
        this.yiMa = yiMa;
        this.jiangXin = jiangXin;
        this.xianChi = xianChi;
        this.huaGai = huaGai;
        this.tianXi = tianXi;
        this.tianYi = tianYi;
    }

    public static DiZhi getDiZhiByName(String name){
        for (DiZhi value : DiZhi.values()) {
            if(Objects.equals(value.getName(), name)){
                return value;
            }
        }
        throw new IllegalArgumentException("No DiZhi found with name: " + name);
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public String getShuXin() {
        return shuXin;
    }

    public String getYiMa() {
        return yiMa;
    }

    public String getJiangXin() {
        return jiangXin;
    }

    public String getXianChi() {
        return xianChi;
    }

    public String getHuaGai() {
        return huaGai;
    }

    public String getTianXi() {
        return tianXi;
    }

    public String getTianYi(){
        return tianYi;
    }

    //返回地支加属性
    public String getNameAndShuXin() {
        return name + shuXin;
    }


}