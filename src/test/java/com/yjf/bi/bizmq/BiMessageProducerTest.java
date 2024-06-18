package com.yjf.bi.bizmq;

import com.yjf.bi.constant.BiMqConstant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import javax.annotation.Resource;

class BiMessageProducerTest {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Test
    void sendMessage() {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, "test");

    }
}