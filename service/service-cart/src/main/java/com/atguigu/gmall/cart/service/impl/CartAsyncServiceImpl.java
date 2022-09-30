package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Async
@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Override
    public Future<CartInfo> updateCartInfo(CartInfo cartInfo) {
        //System.out.println("更新MySQL");
        if (cartInfo.getId() != null) cartInfoMapper.updateById(cartInfo);
        else {
            QueryWrapper<CartInfo> cartInfoWrapper = new QueryWrapper<>();
            cartInfoWrapper.eq("sku_id", cartInfo.getSkuId());
            cartInfoWrapper.eq("user_id", cartInfo.getUserId());
            cartInfoMapper.update(cartInfo, cartInfoWrapper);
        }
        return new AsyncResult<>(cartInfo);
    }

    @Override//返回一个Future用于异常捕获
    public Future<CartInfo> saveCartInfo(CartInfo cartInfo) {
        //System.out.println("插入MySQL");
        cartInfoMapper.insert(cartInfo);
        return new AsyncResult<>(cartInfo);
    }

    @Override
    public void resetCache(RedisTemplate<Object, Object> redisTemplate, String cartKey, String skuId, CartInfo originCartInfo) {
        if (originCartInfo != null)
            redisTemplate.opsForHash().put(cartKey, skuId, originCartInfo);
        else
            redisTemplate.opsForHash().delete(cartKey, skuId);
    }

    @Override
    public void delCartMysql(String userId) {
        QueryWrapper<CartInfo> cartWrapper = new QueryWrapper<>();
        cartWrapper.eq("user_id", userId);
        cartInfoMapper.delete(cartWrapper);
    }

    @Override
    public void delCartRedis(RedisTemplate<Object, Object> redisTemplate, String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override//使用updateWrapper
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        UpdateWrapper<CartInfo> wrapper = new UpdateWrapper<>();
        wrapper.eq("sku_id", skuId).
                eq("user_id", userId);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        cartInfoMapper.update(cartInfo, wrapper);
    }

    @Override
    public void delCartInfoMySql(Long skuId, String userId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("sku_id", skuId);
        cartInfoMapper.delete(wrapper);
    }

}
