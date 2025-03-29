package com.divination.liuyao.util;

import cn.hutool.core.date.ChineseDate;
import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.pojo.dto.BaZi;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 八字相关常量工具类
 */
public class BaZiUtil {
    
    /**
     * 天干数组
     */
    public static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    
    /**
     * 地支数组
     */
    public static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    // 定义天干和地支
    private static final List<String> GAN = Arrays.asList("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸");
    private static final List<String> ZHI = Arrays.asList("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥");

    // 旬首与空亡地支的映射表
    private static final Map<String, String> XUN_KONG_MAP = new HashMap<>();


    /**
     * 天干五行对应
     */
    public static final Map<String, String> TIAN_GAN_WU_XING = new HashMap<>();
    
    /**
     * 地支五行对应
     */
    public static final Map<String, String> DI_ZHI_WU_XING = new HashMap<>();
    
    /**
     * 纳音五行
     */
    public static final Map<String, String> NA_YIN = new HashMap<>();
    
    /**
     * 十神名称
     */
    public static final String[] SHI_SHEN = {"比肩", "劫财", "食神", "伤官", "偏财", "正财", "七杀", "正官", "偏印", "正印"};
    
    /**
     * 十二长生
     */
    public static final String[] CHANG_SHENG = {"长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"};
    
    /**
     * 时辰对应表
     */
    public static final String[] SHI_CHEN = {
        "子时 (23:00-00:59)", "丑时 (01:00-02:59)", "寅时 (03:00-04:59)", "卯时 (05:00-06:59)",
        "辰时 (07:00-08:59)", "巳时 (09:00-10:59)", "午时 (11:00-12:59)", "未时 (13:00-14:59)",
        "申时 (15:00-16:59)", "酉时 (17:00-18:59)", "戌时 (19:00-20:59)", "亥时 (21:00-22:59)"
    };


    /**
     * 根据公历日期时间计算八字
     * @param dateTime 公历日期时间
     * @return 八字信息
     */
    public static BaZi baziConvertByTime(LocalDateTime dateTime) {
        // 转换LocalDateTime为Date
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        // 使用Hutool的ChineseDate
        ChineseDate chineseDate = new ChineseDate(date);

        // 获取年月日三柱
        String cyclicalYMD = chineseDate.getCyclicalYMD();

        // 解析年月日柱
        // 格式通常为"甲子年甲子月甲子日"
        String[] parts = cyclicalYMD.split("年|月|日");
        String yearPillar = parts[0];  // 年柱，如"甲子"
        String monthPillar = parts[1]; // 月柱，如"甲子"
        String dayPillar = parts[2];   // 日柱，如"甲子"

        // 计算时柱
        int hour = dateTime.getHour();
        String hourZhi = BaZiUtil.getHourZhi(hour);
        String hourGan = BaZiUtil.getHourGan(dayPillar.charAt(0) + "", hourZhi);
        String hourPillar = hourGan + hourZhi;

        // 创建并返回BaZi对象
        BaZi baZi = new BaZi();

        baZi.setYearTianGan(TianGan.getTianGanByName(yearPillar.substring(0,1)));
        baZi.setMonthTianGan(TianGan.getTianGanByName(monthPillar.substring(0,1)));
        baZi.setDayTianGan(TianGan.getTianGanByName(dayPillar.substring(0,1)));
        baZi.setHourTianGan(TianGan.getTianGanByName(hourGan));
        baZi.setYearDiZhi(DiZhi.getDiZhiByName(yearPillar.substring(1,2)));
        baZi.setMonthDiZhi(DiZhi.getDiZhiByName(monthPillar.substring(1,2)));
        baZi.setDayDiZhi(DiZhi.getDiZhiByName(dayPillar.substring(1,2)));
        baZi.setHourDiZhi(DiZhi.getDiZhiByName(hourZhi));

        baZi.setYear(yearPillar);
        baZi.setMonth(monthPillar);
        baZi.setDay(dayPillar);
        baZi.setHour(hourPillar);
        baZi.initXunKong();

        return baZi;
    }


    /**
     * 根据用户指定时间，算出对应日支对应的天干
     */
    public static TianGan getTianGanByLocalDateTime(LocalDateTime dateTime){
        // 转换LocalDateTime为Date
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        // 使用Hutool的ChineseDate
        ChineseDate chineseDate = new ChineseDate(date);

        // 获取年月日三柱
        String cyclicalYMD = chineseDate.getCyclicalYMD();

        // 解析年月日柱
        // 格式通常为"甲子年甲子月甲子日"
        String[] parts = cyclicalYMD.split("年|月|日");
        
        // 获取日柱
        String dayCyclical = parts[2];
        
        // 日柱的前一个字符是天干
        String tianGanStr = dayCyclical.substring(0, 1);
        
        // 将字符串转换为TianGan枚举
        for (TianGan tianGan : TianGan.values()) {
            if (tianGan.getName().equals(tianGanStr)) {
                return tianGan;
            }
        }
        return null;
    }

    /**
     * 根据用户指定时间，返回出 年月日时的八字信息和旬空信息
     */
    public static String getAllByLocalDateTime(LocalDateTime dateTime){
        // 转换LocalDateTime为Date
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        // 使用Hutool的ChineseDate
        ChineseDate chineseDate = new ChineseDate(date);

        // 计算时柱
        String cyclicalYMD = chineseDate.getCyclicalYMD();
        String[] parts = cyclicalYMD.split("年|月|日");
        String dayPillar = parts[2];   // 日柱，如"甲子"
        int hour = dateTime.getHour();
        String hourZhi = getHourZhi(hour);
        String hourGan = getHourGan(dayPillar.charAt(0) + "", hourZhi);
        String hourPillar = hourGan + hourZhi;
        return cyclicalYMD + hourPillar + "(日柱" + transitionXunKong(dayPillar) + "旬)";
    }


    public static String transitionXunKong(String timeStr){
        String gan = timeStr.substring(0, 1); // 天干部分
        String zhi = timeStr.substring(1);    // 地支部分

        int ganIndex = GAN.indexOf(gan);
        int zhiIndex = ZHI.indexOf(zhi);

        return XUN_KONG_MAP.getOrDefault("甲" + ZHI.get((zhiIndex - ganIndex + 12) % 12), "??");
    }


    /**
     * 根据小时获取地支
     * @param hour 小时(0-23)
     * @return 对应的地支
     */
    public static String getHourZhi(int hour) {
        int index;
        if (hour == 23 || hour == 0) {
            index = 0; // 子时 23:00-00:59
        } else {
            index = (hour + 1) / 2 % 12;
        }
        return DI_ZHI[index];
    }
    
    /**
     * 根据日干和时支计算时干
     * @param dayGan 日干
     * @param hourZhi 时支
     * @return 时干
     */
    public static String getHourGan(String dayGan, String hourZhi) {
        int dayGanIndex = -1;
        for (int i = 0; i < TIAN_GAN.length; i++) {
            if (TIAN_GAN[i].equals(dayGan)) {
                dayGanIndex = i;
                break;
            }
        }
        
        int hourZhiIndex = -1;
        for (int i = 0; i < DI_ZHI.length; i++) {
            if (DI_ZHI[i].equals(hourZhi)) {
                hourZhiIndex = i;
                break;
            }
        }
        
        // 计算时干索引
        int hourGanIndex = (dayGanIndex % 5 * 2 + hourZhiIndex) % 10;
        return TIAN_GAN[hourGanIndex];
    }


    static {
        XUN_KONG_MAP.put("甲子", "戌亥");
        XUN_KONG_MAP.put("甲戌", "申酉");
        XUN_KONG_MAP.put("甲申", "午未");
        XUN_KONG_MAP.put("甲午", "辰巳");
        XUN_KONG_MAP.put("甲辰", "寅卯");
        XUN_KONG_MAP.put("甲寅", "子丑");

        // 初始化天干五行
        TIAN_GAN_WU_XING.put("甲", "木");
        TIAN_GAN_WU_XING.put("乙", "木");
        TIAN_GAN_WU_XING.put("丙", "火");
        TIAN_GAN_WU_XING.put("丁", "火");
        TIAN_GAN_WU_XING.put("戊", "土");
        TIAN_GAN_WU_XING.put("己", "土");
        TIAN_GAN_WU_XING.put("庚", "金");
        TIAN_GAN_WU_XING.put("辛", "金");
        TIAN_GAN_WU_XING.put("壬", "水");
        TIAN_GAN_WU_XING.put("癸", "水");

        // 初始化地支五行
        DI_ZHI_WU_XING.put("子", "水");
        DI_ZHI_WU_XING.put("丑", "土");
        DI_ZHI_WU_XING.put("寅", "木");
        DI_ZHI_WU_XING.put("卯", "木");
        DI_ZHI_WU_XING.put("辰", "土");
        DI_ZHI_WU_XING.put("巳", "火");
        DI_ZHI_WU_XING.put("午", "火");
        DI_ZHI_WU_XING.put("未", "土");
        DI_ZHI_WU_XING.put("申", "金");
        DI_ZHI_WU_XING.put("酉", "金");
        DI_ZHI_WU_XING.put("戌", "土");
        DI_ZHI_WU_XING.put("亥", "水");

        // 初始化纳音五行
        NA_YIN.put("甲子", "海中金"); NA_YIN.put("乙丑", "海中金");
        NA_YIN.put("丙寅", "炉中火"); NA_YIN.put("丁卯", "炉中火");
        NA_YIN.put("戊辰", "大林木"); NA_YIN.put("己巳", "大林木");
        NA_YIN.put("庚午", "路旁土"); NA_YIN.put("辛未", "路旁土");
        NA_YIN.put("壬申", "剑锋金"); NA_YIN.put("癸酉", "剑锋金");
        NA_YIN.put("甲戌", "山头火"); NA_YIN.put("乙亥", "山头火");
        NA_YIN.put("丙子", "涧下水"); NA_YIN.put("丁丑", "涧下水");
        NA_YIN.put("戊寅", "城头土"); NA_YIN.put("己卯", "城头土");
        NA_YIN.put("庚辰", "白蜡金"); NA_YIN.put("辛巳", "白蜡金");
        NA_YIN.put("壬午", "杨柳木"); NA_YIN.put("癸未", "杨柳木");
        NA_YIN.put("甲申", "泉中水"); NA_YIN.put("乙酉", "泉中水");
        NA_YIN.put("丙戌", "屋上土"); NA_YIN.put("丁亥", "屋上土");
        NA_YIN.put("戊子", "霹雳火"); NA_YIN.put("己丑", "霹雳火");
        NA_YIN.put("庚寅", "松柏木"); NA_YIN.put("辛卯", "松柏木");
        NA_YIN.put("壬辰", "长流水"); NA_YIN.put("癸巳", "长流水");
        NA_YIN.put("甲午", "砂石金"); NA_YIN.put("乙未", "砂石金");
        NA_YIN.put("丙申", "山下火"); NA_YIN.put("丁酉", "山下火");
        NA_YIN.put("戊戌", "平地木"); NA_YIN.put("己亥", "平地木");
        NA_YIN.put("庚子", "壁上土"); NA_YIN.put("辛丑", "壁上土");
        NA_YIN.put("壬寅", "金箔金"); NA_YIN.put("癸卯", "金箔金");
        NA_YIN.put("甲辰", "覆灯火"); NA_YIN.put("乙巳", "覆灯火");
        NA_YIN.put("丙午", "天河水"); NA_YIN.put("丁未", "天河水");
        NA_YIN.put("戊申", "大驿土"); NA_YIN.put("己酉", "大驿土");
        NA_YIN.put("庚戌", "钗环金"); NA_YIN.put("辛亥", "钗环金");
        NA_YIN.put("壬子", "桑柘木"); NA_YIN.put("癸丑", "桑柘木");
        NA_YIN.put("甲寅", "大溪水"); NA_YIN.put("乙卯", "大溪水");
        NA_YIN.put("丙辰", "沙中土"); NA_YIN.put("丁巳", "沙中土");
        NA_YIN.put("戊午", "天上火"); NA_YIN.put("己未", "天上火");
        NA_YIN.put("庚申", "石榴木"); NA_YIN.put("辛酉", "石榴木");
        NA_YIN.put("壬戌", "大海水"); NA_YIN.put("癸亥", "大海水");
    }
} 