package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {

    String createAliPay(Long orderId) throws AlipayApiException;

    boolean refund(Long orderId);

    boolean closePay(Long orderId);

    boolean checkPayment(Long orderId);
}
