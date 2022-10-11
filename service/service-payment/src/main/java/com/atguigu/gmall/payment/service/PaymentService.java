package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    void paySuccess(String outTradeNo, Map<String,String> paramMap,String paymentType);

    void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo);

    void closePayment(Long orderId);
}
