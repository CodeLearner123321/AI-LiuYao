package com.divination.liuyao.pojo.entity;

import com.divination.liuyao.common.annotation.AIDoc;
import com.divination.liuyao.common.annotation.AIDocField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AIDoc("整个卦与问题的基本信息类，分为问题背景，卦象和时间。在根节点才会存放具体的信息且每一个数据都有根节点(如果没有根节点信息，则为空字符串:'')")
public class Prediction {

    private Description description;
    private Gua gua;
    private Time time;

    public boolean check() {
        return description != null && gua != null && time != null;
    }

    @Data
    @AIDoc("问题的基本信息")
    public static class Description {
        @AIDocField(desc = "该问题", required = false)
        private String question;
        @AIDocField(desc = "问题的背景", required = false)
        private String background;
    }

    @Data
    @AIDoc("卦象信息")
    public static class Gua {
        @AIDocField(desc = "主卦,六十四卦中的某一卦", required = true)
        private String zhuGua;
        @AIDocField(desc = "变卦,六十四卦中的某一卦,可能没有变卦", required = false)
        private String bianGua;
    }

    @Data
    public static class Time {
        @AIDocField(desc = "年的时间信息（包括干支）", required = false)
        private DatePart year;
        @AIDocField(desc = "月的时间信息（包括干支）", required = false)
        private DatePart month;
        @AIDocField(desc = "日的时间信息（包括干支）", required = false)
        private DatePart day;
        @AIDocField(desc = "时的时间信息（包括干支）", required = false)
        private DatePart hour;
    }

    @Data
    public static class DatePart {
        @AIDocField(desc = "干支信息", required = false)
        private Ganzhi ganzhi;
        @AIDocField(desc = "时间信息，比如：在年上就是2012、2045等表示年份的日期，在月上就是1、2、10、12等表示月份的信息，在日上就是1、12、60等表示日期的信息，在时上就是1、3、23、60等表示小时的信息", required = false)
        private String time;
    }

    @Data
    public static class Ganzhi {
        @AIDocField(desc = "天干信息,在{甲,乙,丙,丁,戊,己,庚,辛,壬,癸}中取一个", required = false)
        private String tiangan;
        @AIDocField(desc = "地支信息,在{子,丑,寅,卯,辰,巳,午,未,申,酉,戌,亥}中取一个", required = false)
        private String dizhi;
    }
}
