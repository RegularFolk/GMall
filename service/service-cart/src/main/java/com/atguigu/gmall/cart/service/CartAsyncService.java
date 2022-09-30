package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Future;

public interface CartAsyncService {

    Future<CartInfo> updateCartInfo(CartInfo cartInfo);

    Future<CartInfo> saveCartInfo(CartInfo cartInfo);

    void resetCache(RedisTemplate<Object, Object> redisTemplate, String cartKey, String skuId, CartInfo originCartInfo);

    void delCartMysql(String userId);

    void delCartRedis(RedisTemplate<Object, Object> redisTemplate, String cartKey);

    void checkCart(String userId, Integer isChecked, Long skuId);

    void delCartInfoMySql(Long skuId, String userId);
}
