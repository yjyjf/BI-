//package com.yjf.bi.bizmq;
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//import com.yjf.bi.constant.BiMqConstant;
//
///**
// * 用于创建程序用到的交换机和队列（只用在程序启动前执行一次）
// */
//public class BiInitMain {
//
//    public static void main(String[] args) {
//        try {
//            // 创建连接工厂
//            ConnectionFactory factory = new ConnectionFactory();
//            // 设置RabbitMQ连接参数
//            factory.setHost("192.168.88.130");
//            factory.setPort(5672);
//            factory.setUsername("yjf");
//            factory.setPassword("yjf");
//
//            // 创建连接
//            Connection connection = factory.newConnection();
//            // 创建通道
//            Channel channel = connection.createChannel();
//            // 定义交换机的名称为"bi_exchange"
//            String EXCHANGE_NAME = BiMqConstant.BI_EXCHANGE_NAME;
//            // 声明交换机，指定交换机类型为 direct
//            channel.exchangeDeclare(EXCHANGE_NAME, "direct",true);
//
//            // 创建队列，bi_queue
//            String queueName = BiMqConstant.BI_QUEUE_NAME;
//            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 null
//            channel.queueDeclare(queueName, true, false, false, null);
//            // 将队列绑定到交换机，指定路由键为 "bi_routingKey"
//            channel.queueBind(queueName, EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);
//        } catch (Exception e) {
//            // 异常处理
//        }
//    }
//}
//
