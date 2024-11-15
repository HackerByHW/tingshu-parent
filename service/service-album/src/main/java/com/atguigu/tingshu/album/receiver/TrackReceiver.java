package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ClassName: TrackReceiver
 * Package: com.atguigu.tingshu.album.receiver
 * Description:
 *
 * @Author Hacker by HW
 * @Create 2024/11/15 19:06
 * @Version 1.0
 */
@Slf4j
@Component
public class TrackReceiver {

    @Autowired
    private TrackInfoService trackInfoService;
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(value = MqConst.EXCHANGE_TRACK,durable = "true"),
    value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE,durable = "true"),
    key = {MqConst.ROUTING_TRACK_STAT_UPDATE}
    ))
    public void updatePalyNum(String content, Message message, Channel channel){
    if (StringUtils.hasText(content)){
        // string --  TrackStatMqVo
        TrackStatMqVo trackStatMqVo = JSON.parseObject(content, TrackStatMqVo.class);
        //调用方法更新播放量
        trackInfoService.updatePalyNum(trackStatMqVo);
    }
    channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
