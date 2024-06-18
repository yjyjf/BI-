package com.yjf.bi.bizmq;

import com.rabbitmq.client.Channel;
import com.yjf.bi.common.ErrorCode;
import com.yjf.bi.constant.BiMqConstant;
import com.yjf.bi.exception.BusinessException;
import com.yjf.bi.model.entity.Chart;
import com.yjf.bi.model.entity.User;
import com.yjf.bi.model.enums.ChartStatusEnum;
import com.yjf.bi.retry.GuavaRetrying;
import com.yjf.bi.service.ChartService;
import com.yjf.bi.service.UserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BIMessageConsumer {
    @Resource
    private ChartService chartService;
    @Resource
    private UserService userService;
    @Resource
    private GuavaRetrying guavaRetrying;


    /**
     * 接收消息
     * 由 RabbitMQ 消费者框架自动调用。
     * 当有消息发布到 BiMqConstant.BI_QUEUE_NAME 队列时，
     * RabbitMQ 会将消息传递给这个 receiveMessage 函数进行处理。
     *
     * @param message     消息
     * @param channel     通道
     * @param deliveryTag 消息的标识
     */
    //注解功能:允许你在不声明throws关键字的情况下，隐式地处理可能会抛出的检查性异常
    @SneakyThrows
    // 使用@RabbitListener注解指定要监听的队列名称为"code_queue"，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        // 使用日志记录器打印接收到的消息内容
        log.info("receiveMessage message = {}", message);
        if(StringUtils.isBlank(message)){
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        // 根据消息队列中消息获取图表id
        long chartId = Long.parseLong(message);
        // 根据图表id从数据库中获取图表
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            // 如果图表为空，拒绝消息并抛出业务异常
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        Long userId = chart.getUserId();
        // / 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
        Chart running = new Chart();
        running.setId(chartId);
        running.setStatus(ChartStatusEnum.RUNNING.getValue());
        boolean runningResult = chartService.updateById(running);
        if(!runningResult) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chartId, "更新图表执行中状态失败");
            return ;
        }

        // 直接调用ai
//        String result = aiManager.doChat();
        // 使用for循环最多调用三次ai
//        String result = chartService.validAiResult(chartService.buildUserInput(chart));

        // 使用guava retrying 进行重试，如果ai生成的代码错误则重试，最多重试三次
        String result = guavaRetrying.retryDoChart(chartService.buildUserInput(chart));
        System.out.println("============================\n"+"这里是mq,result="+result);
        if(result == null){
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chartId,"AI 生成错误");
            // 放入死信队列
        }
        // 用validAiResult方法调用得到的结果必定为正确的json格式代码，否则抛出生成失败异常
        String[] splits = result.split("#####");
        if(splits.length < 3){
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chartId,"AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();


        Chart succeed = new Chart();
        succeed.setId(chartId);
        succeed.setGenChart(genChart);
        succeed.setGenResult(genResult);
        succeed.setStatus(ChartStatusEnum.SUCCEED.getValue());
        boolean succeedResult = chartService.updateById(succeed);
        if(!succeedResult) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chartId, "更新图表成功状态失败");
            return ;
        }

        // 异步
        // 消费者执行后用户剩余次数-1
        User producerUser = userService.getById(userId);
        Integer leftNum = producerUser.getLeftNum();
        Integer totalNum = producerUser.getTotalNum();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setLeftNum(leftNum - 1);
        updateUser.setTotalNum(totalNum + 1);
        // 更新用户信息
        if(!userService.updateById(updateUser)){
            log.error("用户id={}：使用次数减少失败！",userId);
            channel.basicNack(deliveryTag, false, false);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            return ;
        }

        // 投递标签是一个数字标识,它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态。通过将投递标签传递给channel.basicAck(deliveryTag, false)方法,可以告知RabbitMQ该消息已经成功处理,可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag, false);
    }


}
