package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @SneakyThrows(IOException.class)
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) {//接收到则进行取消订单的业务逻辑，对超时依旧UNPAID的订单进行取消
        if (orderId != null) {
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null &&
                    "UNPAID".equals(orderInfo.getOrderStatus()) &&
                    "UNPAID".equals(orderInfo.getProcessStatus())) {//修改订单状态
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (paymentInfo != null && "UNPAID".equals(paymentInfo.getPaymentStatus())) {//电商本地有交易记录
                    if (!paymentFeignClient.checkPayment(orderId) || paymentFeignClient.closePay(orderId)) {
                        //如果支付宝不存在交易或者存在交易但未支付(即交易关闭成功)，则需要本地关闭paymentInfo
                        orderService.execExpireOrder(orderId, true);
                    }
                    //如果运行到此说明在本地关闭前一瞬间用户在支付宝完成交易，数据库中的paymentInfo的状态已经是PAID，则不需要进行任何操作
                } else {//不存在unpaid的交易记录，只关闭order
                    orderService.execExpireOrder(orderId, false);
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
//                    boolean checkPayment = paymentFeignClient.checkPayment(orderId);
//                    if (checkPayment) {//支付宝有交易记录
//                        boolean closePay = paymentFeignClient.closePay(orderId);
//                        if (closePay) {//关闭支付宝交易成功，用户未支付
//                            orderService.execExpireOrder(orderId, true);
//                        }
//                        //若关闭支付宝交易失败，用户已支付,即订单取消的前一瞬间用户付钱了
//                    } else {//支付宝没有交易记录，关闭本地
//                        orderService.execExpireOrder(orderId, true);
//                    }

    @SneakyThrows(IOException.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))//监听订单消息
    public void paymentPay(Long orderId, Message message, Channel channel) {
        if (orderId != null) {//更新订单状态
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null &&
                    "UNPAID".equals(orderInfo.getOrderStatus()) &&
                    "UNPAID".equals(orderInfo.getProcessStatus())) {//修改订单状态
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                //发送消息给库存，减少库存数量
                orderService.sendOrderStatus(orderId);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    @SneakyThrows(IOException.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))//监听库存系统发送的减库存系统
    public void updateOrderStatus(String wareJson, Message message, Channel channel) {
        if (!StringUtils.isEmpty(wareJson)) {
            Map<String, String> map = JSON.parseObject(wareJson, Map.class);
            String orderId = map.get("orderId");
            String status = map.get("status");
            if ("DEDUCTED".equals(status)) {//减库存成功,更新订单状态
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {//异常情况
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
