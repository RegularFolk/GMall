package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Configuration
public class ConfirmReceiver {

    @SneakyThrows(IOException.class)//将指定Exception包装为一个RuntimeException再抛出，默认为所有exception，仅此而已，作用是使代码看起来更简洁
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "queue.confirm",
                            autoDelete = "false",
                            durable = "true"
                    ),
                    exchange = @Exchange(value = "exchange.confirm"),
                    key = {"routing.confirm"}
            )
    )
    public void process(Message message, Channel channel) {
        try {
            System.out.println("RabbitListener:" + new String(message.getBody()));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

}
