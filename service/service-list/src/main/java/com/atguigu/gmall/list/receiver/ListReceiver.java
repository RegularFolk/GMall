package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component//消费消息
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    //商品上架更新到ES
    @SneakyThrows(IOException.class)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = MqConst.QUEUE_GOODS_UPPER,
                            durable = "true"
                    ),
                    exchange = @Exchange(
                            value = MqConst.EXCHANGE_DIRECT_GOODS),
                    key = {MqConst.ROUTING_GOODS_UPPER}
            )
    )
    public void upperGoods(Long skuId, Message message, Channel channel) {
        if (skuId != null) searchService.upperGoods(skuId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //商品下架更新到ES
    @SneakyThrows(IOException.class)
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = MqConst.QUEUE_GOODS_LOWER,
                            durable = "true"
                    ),
                    exchange = @Exchange(
                            value = MqConst.EXCHANGE_DIRECT_GOODS),
                    key = {MqConst.ROUTING_GOODS_LOWER}
            )
    )
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        if (skuId != null) searchService.lowerGoods(skuId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
