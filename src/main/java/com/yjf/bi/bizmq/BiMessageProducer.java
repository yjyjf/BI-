package com.yjf.bi.bizmq;

import com.yjf.bi.constant.BiMqConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param message 把chartId作为参数，传入到消息队列中
     */
    public void sendMessage(String  message){
        //Direct交换机 通过指定的交换机和路由键，将消息路由到指定的队列中
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY,message);
    }
}
