package com.divination.liuyao.pojo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Prediction {

    private Description description;
    private Gua gua;
    private Time time;

    @Data
    public static class Description {
        private String question;
        private String background;
    }

    @Data
    public static class Gua {
        private String zhuGua;
        private String bianGua;
    }

    @Data
    public static class Time {
        private DatePart year;
        private DatePart month;
        private DatePart day;
        private DatePart hour;
    }

    @Data
    public static class DatePart {
        private Ganzhi ganzhi;
        private String time;
    }

    @Data
    public static class Ganzhi {
        private String tiangan;
        private String dizhi;
    }
}
