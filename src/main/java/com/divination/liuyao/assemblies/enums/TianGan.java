package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public enum TianGan {
    JIA("甲","木","寅","巳","卯","丑,未"),
    YI("乙","木","卯","午","寅","子,申"),
    BING("丙","火","巳","申","午","亥,酉"),
    DIN("丁","火","午","酉","巳","亥,酉"),
    WU("戊","土","巳","申","午","丑,未"),
    JI("己","土","午","酉","巳","子,申"),
    GENG("庚","金","申","亥","酉","寅,午"),
    XIN("辛","金","酉","子","申","寅,午"),
    REN("壬","水","亥","寅","子","卯,巳"),
    GUI("癸","水","子","卯","亥","卯,巳");

    private final String name;

    private final String shuXin;

    //禄神
    private final String lushen;

    //文昌
    private final String wenChang;

    //羊刃
    private final String yangRen;

    //贵人
    private final String guiRen;

    // 构造方法
    TianGan(String name, String shuXin, String lushen, String wenChang, String yangRen, String guiRen) {
        this.name = name;
        this.shuXin = shuXin;
        this.lushen = lushen;
        this.wenChang = wenChang;
        this.yangRen = yangRen;
        this.guiRen = guiRen;
    }

    public static TianGan getTianGanByName(String name){
        for (TianGan value : TianGan.values()) {
            if(Objects.equals(value.name, name)){
                return value;
            }
        }
        return null;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public String getWenChang() {
        return wenChang;
    }

    public String getLuShen() {
        return lushen;
    }

    public String getYangRen() {
        return yangRen;
    }

    public String getGuiRen() {
        return guiRen;
    }

}