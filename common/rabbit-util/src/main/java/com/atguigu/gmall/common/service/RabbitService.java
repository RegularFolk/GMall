package com.atguigu.gmall.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 指定交换机和路由发送message
     * @param exchange 交换机
     * @param routeKey 路由键
     * @param message 发送的消息
     * @return true
     */
    public boolean sendMessage(String exchange, String routeKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routeKey, message);
        return true;
    }

    /**
     * 指定交换机和路由键发送延时消息
     * @param exchange 交换机
     * @param routeKey 路由键
     * @param message 发送的消息
     * @param delayTime 延迟的时间，单位秒
     * @return TRUE
     */
    public boolean sendDelayMessage(String exchange, String routeKey, Object message, int delayTime) {
        rabbitTemplate.convertAndSend(exchange, routeKey, message, processMessage -> {
            processMessage.getMessageProperties().setDelay(delayTime * 1000);//设置延时时间，单位为秒
            return processMessage;
        });
        return true;
    }

}
