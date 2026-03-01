package com.divination.liuyao.test;

import java.util.concurrent.CountDownLatch;

public class ThreadExceptionDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("A 主线程开始");

        CountDownLatch latch = new CountDownLatch(1);

        Thread bThread = new Thread(() -> {
            System.out.println("B 线程开始");

            // B 调用 C（异步，不等待）
            callCAsync();

            System.out.println("B 线程即将结束");
            latch.countDown();
        }, "B-Thread");

        bThread.start();

        // A 等待 B 执行完成
        latch.await();

        System.out.println("A 主线程结束");
    }

    private static void callCAsync() {
        Thread cThread = new Thread(() -> {
            System.out.println("C 线程开始");

            int a = 10/0;
            System.out.println("C 线程结束");


        }, "C-Thread");

        cThread.start();
    }
}
