package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;
import java.util.concurrent.Future;

public interface OrderAsyncService {
    Future<List<UserAddress>> findUserAddressListByUserIdAsync(String userId);

    Future<List<CartInfo>> getCartCheckedListAsync(String userId);

    void insertOrderDetailAsync(OrderDetail orderDetail);

    Future<OrderInfo> insertAsync(OrderInfo orderInfo);

    void deleteTradeNo(String userId);
}
