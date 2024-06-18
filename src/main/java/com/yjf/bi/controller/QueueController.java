package com.yjf.bi.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池测试代码
 */
@RestController
@RequestMapping("/queue")
@Profile({"dev","local"})//用于测试，仅在开发环境下生效
@Slf4j
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @GetMapping("/add")
    //接收一个参数name,然后将任务添加到线程池中
    public void add(String name){
        //使用completableFuture的runAsync方法，执行一个异步任务
        CompletableFuture.runAsync(()->{
            log.info("任务执行中："+name+".执行人:"+Thread.currentThread().getName());
            try{
                //休眠10分钟，模拟长时间运行的任务
                Thread.sleep(600000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            //异步任务在threadPoolExecutor中执行

        },threadPoolExecutor);
    }


    @GetMapping("/get")
    public String get(){
        //创建一个HashMap存储线程池的状态信息
        Map<String,Object> map= new HashMap<>();
        //获取线程池的队列长度,并写入map中
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度",size);

        //获取线程池已接受的任务总数
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数",taskCount);

        //获取线程池已完成的任务数
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数",completedTaskCount);

        //获取线程池中正在执行任务的线程数
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在工作的线程数",activeCount);

        //将map转换为json字符串并返回
        return JSONUtil.toJsonStr(map);
    }

}
