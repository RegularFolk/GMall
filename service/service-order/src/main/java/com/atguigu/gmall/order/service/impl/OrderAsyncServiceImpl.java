package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderAsyncService;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;

@Service
@Async
public class OrderAsyncServiceImpl implements OrderAsyncService {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public Future<List<UserAddress>> findUserAddressListByUserIdAsync(String userId) {
        List<UserAddress> userAddresses = userFeignClient.findUserAddressListByUserId(userId);
        return new AsyncResult<>(userAddresses);
    }

    @Override
    public Future<List<CartInfo>> getCartCheckedListAsync(String userId) {
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId).getData();
        return new AsyncResult<>(cartCheckedList);
    }

    @Override
    public void insertOrderDetailAsync(OrderDetail orderDetail) {
        orderDetailMapper.insert(orderDetail);
    }

    @Override
    public Future<OrderInfo> insertAsync(OrderInfo orderInfo) {
        orderInfoMapper.insert(orderInfo);
        return new AsyncResult<>(orderInfo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeCode";
        redisTemplate.delete(tradeNoKey);
    }
}
