package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        Long orderInfoId = orderInfo.getId();
        wrapper.eq("order_id", orderInfoId).
                eq("payment_type", paymentType);
        Integer count = paymentMapper.selectCount(wrapper);
        if (count > 0) return;//已经保存过
        PaymentInfo paymentInfo = packPayment(orderInfo, paymentType);
        paymentMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no", outTradeNo).
                eq("payment_type", paymentType);
        return paymentMapper.selectOne(wrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, Map<String, String> paramMap, String paymentType) {
        //交易记录状态如果是CLOSED或者PAID，则直接返回不需要继续处理
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, paymentType);
        if ("CLOSED".equals(paymentInfoQuery.getPaymentStatus()) || "PAID".equals(paymentInfoQuery.getPaymentStatus())) {
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        String tradeNo = paramMap.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramMap.toString());
        updatePaymentInfo(outTradeNo, paymentType, paymentInfo);
        //发送消息给order，异步修改对应orderInfo的状态,传递orderId和outTradeNo均可，两者都是唯一的
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfoQuery.getOrderId());
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        UpdateWrapper<PaymentInfo> wrapper = new UpdateWrapper<>();
        wrapper.eq("out_trade_no", outTradeNo).eq("payment_type", paymentType);
        paymentMapper.update(paymentInfo, wrapper);
    }

    @Override//关闭交易记录
    public void closePayment(Long orderId) {
        UpdateWrapper<PaymentInfo> wrapper = new UpdateWrapper<>();
        wrapper.eq("order_id", orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentMapper.update(paymentInfo, wrapper);
    }

    private PaymentInfo packPayment(OrderInfo orderInfo, String paymentType) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        return paymentInfo;
    }
}
