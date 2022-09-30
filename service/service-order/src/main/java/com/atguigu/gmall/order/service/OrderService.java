package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;

public interface OrderService {

    Long saveOrderInfo(OrderInfo orderInfo);

    boolean checkTradeCode(String userId, String tradeNo);

    String getTradeNo(String userId);

    boolean checkStock(Long skuId, Integer skuNum);
}
