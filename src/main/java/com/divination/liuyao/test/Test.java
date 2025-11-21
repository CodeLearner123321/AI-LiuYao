package com.divination.liuyao.test;

import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.util.AIDocJsonBuilder;

public class Test {
    public static void main(String[] args) {
        long start = System.currentTimeMillis(); // 开始时间

        String doc = AIDocJsonBuilder.generateJsonWithNotes(Prediction.class);
        System.out.println(doc);

        long end = System.currentTimeMillis(); // 结束时间


        System.out.println("生成耗时1：" + (end - start) + " ms");


    }
}
