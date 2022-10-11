package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsAsync;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Async
@Service
public class SeckillGoodsAsyncImpl implements SeckillGoodsAsync {

    @Override
    public void updateSeckillStockAsync(SeckillGoodsMapper seckillGoodsMapper, SeckillGoods seckillGoods) {
        seckillGoodsMapper.updateById(seckillGoods);
    }

    @Override
    public void updateSeckillStockRedisAsync(RedisTemplate<Object, Object> redisTemplate, SeckillGoods seckillGoods, Long skuId) {
        redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(), seckillGoods);
    }
}
