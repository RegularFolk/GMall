package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
@Slf4j
@Api("消息队列控制器")
public class MqController {
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @ApiOperation("测试消息发送")
    @GetMapping("sendConfirm")
    public Result<Object> sendConfirm() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean flag = rabbitService.sendMessage("exchange.confirm", "routing.confirm", sdf.format(new Date()));
        if (!flag) return Result.fail();
        return Result.ok();
    }

    @ApiOperation("测试发送死信")
    @GetMapping("sendDeadLetter")
    public Result<Object> sendDeadLetter() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "ok");
        System.out.println(sdf.format(new Date()) + " Delay sent.");
        return Result.ok();
    }

    @ApiOperation("测试延时插件")
    @GetMapping("sendDelay")
    public Result<Object> sendDelay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, sdf.format(new Date()), message -> {
            message.getMessageProperties().setDelay(10 * 1000);
            System.out.println(sdf.format(new Date()) + " Delay sent.");
            return message;
        });
        return Result.ok();
    }

}
