package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {

    /**
     * 获取所有秒杀商品
     *
     * @return 秒杀商品集合
     */
    List<SeckillGoods> findAll();

    /**
     * 根据skuId查询对应的秒杀商品
     * @param skuId skuId
     * @return 秒杀商品
     */
    SeckillGoods findSeckillGoodsById(Long skuId);

    /**
     * 预下单
     * @param skuId skuId
     * @param userId userId
     */
    void seckillOrder(Long skuId, String userId);

    /**
     * 页面轮询用户秒杀状态
     * @param skuId skuId
     * @param userId userId
     * @return 秒杀状态
     */
    Result<Object> checkOrder(Long skuId, String userId);
}
