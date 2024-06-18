package com.yjf.bi.bizmq;

import com.rabbitmq.client.*;

public class RabbitMQTest {

    private static final String QUEUE_NAME = "test_queue";
    private static final String EXCHANGE_NAME = "test_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.88.130"); // 根据实际情况修改为主机地址
        factory.setPort(15672); // 默认端口
        factory.setUsername("yjf"); // 默认用户名
        factory.setPassword("yjf"); // 默认密码

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // 声明交换器
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);

            // 可以进一步测试队列声明、消息发送等操作
            System.out.println("Connected to RabbitMQ successfully!");
            System.out.println("Exchange declared: " + EXCHANGE_NAME);

            // 示例：声明一个队列（可选）
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            System.out.println("Queue declared: " + QUEUE_NAME);

            // 发送一条测试消息（可选）
            String message = "Hello, RabbitMQ!";
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
            System.out.println("Sent message: '" + message + "'");

        } catch (Exception e) {
            System.err.println("Error occurred while connecting to RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
