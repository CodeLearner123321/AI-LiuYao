package com.divination.liuyao.util;

import java.lang.ModuleLayer.Controller;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于处理五行生克
 */
public class WuXinUtil {
    private static final Map<String, Integer> wuXinMap = new HashMap<>();
    private static final String[] liuYao = new String[]{"兄弟","子孙", "妻财", "官鬼", "父母"};


    /**
     * 通过属性，获取到五行对应的关系（"兄弟","子孙", "妻财", "官鬼", "父母"）
     */
    public static String relationLiuYao(String zhuShuXin, String passivityShuXin){
        if(!wuXinMap.containsKey(zhuShuXin) || !wuXinMap.containsKey(passivityShuXin)){
            return "??";
        }

        Integer zhuNumber = wuXinMap.get(zhuShuXin);
        Integer passivityNumber = wuXinMap.get(passivityShuXin);

        for (int i = 0; i < 5; i++) {
            if( passivityNumber == ((zhuNumber + i) % 5) ){
                return liuYao[i];
            }
        }

        return "??";
    }


    static {
        wuXinMap.put("木",0);
        wuXinMap.put("火",1);
        wuXinMap.put("土",2);
        wuXinMap.put("金",3);
        wuXinMap.put("水",4);
    }
}
