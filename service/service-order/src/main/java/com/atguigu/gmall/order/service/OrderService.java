package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    Long saveOrderInfo(OrderInfo orderInfo);

    boolean checkTradeCode(String userId, String tradeNo);

    String getTradeNo(String userId);

    boolean checkStock(Long skuId, Integer skuNum);

    void execExpireOrder(Long orderId);

    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    OrderInfo getOrderInfo(Long orderId);

    void sendOrderStatus(Long orderId);

    Map<String, Object> getMapByOrderInfo(OrderInfo orderInfo);

    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 关闭过期订单
     * @param orderId orderId
     * @param flag 是否同时关闭paymentInfo
     */
    void execExpireOrder(Long orderId, boolean flag);
}

