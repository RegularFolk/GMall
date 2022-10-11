package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.data.redis.core.RedisTemplate;

public interface SeckillGoodsAsync {

    void updateSeckillStockAsync(SeckillGoodsMapper seckillGoodsMapper, SeckillGoods seckillGoods);

    void updateSeckillStockRedisAsync(RedisTemplate<Object, Object> redisTemplate, SeckillGoods seckillGoods,Long skuId);
}
