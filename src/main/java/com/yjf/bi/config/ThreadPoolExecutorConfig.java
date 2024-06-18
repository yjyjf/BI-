package com.yjf.bi.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 */
@Configuration
public class ThreadPoolExecutorConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //创建一个线程工厂
        ThreadFactory threadFactory =new ThreadFactory() {
            private int cnt=1;

            // 创建并初始化一个线程
            // @NotNull Runnable runnable 表示方法参数 runnable 应该永远不为null，
            // 如果这个方法被调用的时候传递了一个null参数，就会报错
            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                Thread thread= new Thread(runnable);
                thread.setName("线程"+ cnt ++);
                return thread;
            }

        };
        //创建一个新的线程池，线程池核心大小为2，最大线程池为4
        //非核心线程空闲时间为100秒，任务队列为阻塞队列，长度为4，使用自定义的线程工厂创建线程
        //先占用 核心线程2->再 堵满 阻塞队列4->再创建并占用非核心线程->根据配置的饱和策略来处理新任务（丢弃或其他策略）：
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100, TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),threadFactory);

        return threadPoolExecutor;
    }
}
