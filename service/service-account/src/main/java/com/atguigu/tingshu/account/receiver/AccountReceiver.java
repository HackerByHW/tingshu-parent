package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            key = {MqConst.ROUTING_USER_REGISTER}
    ))
    public void addUserAccount(Long userId, Message message, Channel channel) {
        if(null != userId) {
            userAccountService.initUserAccount(userId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
