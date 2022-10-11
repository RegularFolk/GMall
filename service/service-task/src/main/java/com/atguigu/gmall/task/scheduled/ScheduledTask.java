package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //0 0 1 * * ? 每天凌晨一点执行一次
    @Scheduled(cron = "0/10 * * * * ?")//测试每隔十秒执行执行,向rabbitMQ发送一条消息，在service-activity监听
    public void checkSeckill() {
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "ding");
        System.out.println("》》》秒杀定时消息发送!《《《");
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void endSeckill() {
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "dong");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("清理秒杀活动！");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>");
    }

}
