package com.divination.liuyao.pojo.dto;

import static com.divination.liuyao.util.BaZiUtil.transitionXunKong;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.util.BaZiUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaZi {
    private String year;      // 年柱
    private String month;     // 月柱
    private String day;       // 日柱
    private String hour;      // 时柱

    //旬空信息
    private String yearToNull;
    private String monthToNull;
    private String dayToNull;
    private String hourToNull;

    //年月日时的天干和地支
    @JsonIgnore
    private TianGan yearTianGan;
    @JsonIgnore
    private TianGan monthTianGan;
    @JsonIgnore
    private TianGan dayTianGan;
    @JsonIgnore
    private TianGan hourTianGan;
    @JsonIgnore
    private DiZhi yearDiZhi;
    @JsonIgnore
    private DiZhi monthDiZhi;
    @JsonIgnore
    private DiZhi dayDiZhi;
    @JsonIgnore
    private DiZhi hourDiZhi;

    public void initXunKong(){
        if(year == null && yearTianGan != null && yearDiZhi != null){
            year = yearTianGan.getName() + yearDiZhi.getName();
        }
        if(month == null && monthTianGan != null && monthDiZhi != null){
            month = monthTianGan.getName() + monthDiZhi.getName();
        }
        if(day == null && dayTianGan != null && dayDiZhi != null){
            day = dayTianGan.getName() + dayDiZhi.getName();
        }
        if(hour == null && hourTianGan != null && hourDiZhi != null){
            hour = hourTianGan.getName() + hourDiZhi.getName();
        }
        if(day != null){
            this.dayToNull = BaZiUtil.transitionXunKong(day);
        }

        if(year == null || month == null || day == null || hour == null ||
            year.isEmpty() || month.isEmpty() || day.isEmpty() || hour.isEmpty()){
            return;
        }
        this.yearToNull = BaZiUtil.transitionXunKong(year);
        this.monthToNull = BaZiUtil.transitionXunKong(month);
        this.dayToNull = BaZiUtil.transitionXunKong(day);
        this.hourToNull = BaZiUtil.transitionXunKong(hour);
    }

    @Override
    public String toString() {
        return year == null ? "" : "年柱" + year + " " +
            (month == null ? "" : "月柱" + month) + " " + (monthToNull == null ? "" : "(" + monthToNull) + "旬)" +
            (day == null ? "" : "日柱" + day) + " " +
            (hour == null ? "" : "时柱" + hour) + "\n"
                ;
    }
}