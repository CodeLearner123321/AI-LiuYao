package com.divination.liuyao.test;

import java.util.concurrent.*;

public class CompletableFutureExceptionDemo {

    // 统一线程池（模拟生产环境）
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws Exception {
        System.out.println("A 主线程开始");

        // A 调用 B（A 等待 B）
        CompletableFuture<Void> bFuture =
                CompletableFuture.runAsync(() -> b(), EXECUTOR);

        // A 等待 B 执行完成
        bFuture.join();

        System.out.println("A 主线程结束");

        // 防止 JVM 过早退出，看清 C 的异常
        Thread.sleep(2000);
        EXECUTOR.shutdown();
    }

    // ========== B ==========
    private static void b() {
        System.out.println("B 线程开始：" + Thread.currentThread().getName());

        // B 调用 C（不等待）
        CompletableFuture.runAsync(() -> c(), EXECUTOR);

        System.out.println("B 线程结束");
    }

    // ========== C ==========
    private static void c() {
        System.out.println("C 线程开始：" + Thread.currentThread().getName());

        // 模拟异常
        if (true) {
            throw new RuntimeException("C 线程发生异常！");
        }
    }
}
