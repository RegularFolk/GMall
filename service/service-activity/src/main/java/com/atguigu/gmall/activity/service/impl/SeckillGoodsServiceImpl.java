package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsAsync;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillGoodsAsync seckillGoodsAsync;

    @Override
    public List<SeckillGoods> findAll() {
        List<Object> values = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        if (CollectionUtils.isEmpty(values)) return null;
        ArrayList<SeckillGoods> seckillGoods = new ArrayList<>(values.size());
        values.forEach(value -> seckillGoods.add((SeckillGoods) value));
        return seckillGoods;
    }

    @Override
    public SeckillGoods findSeckillGoodsById(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    /*
     * 1.判断状态位(是否已售罄)
     * 2.控制重复下单
     * 3.获取队列中的商品
     * 4.订单记录存入缓存
     * 5.对商品更新库存
     * */
    @Override
    public void seckillOrder(Long skuId, String userId) {
        String state = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(state)) return;//售罄
        String userOrderKey = RedisConst.SECKILL_USER + userId;
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(userOrderKey, skuId, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(flag)) return;//已经下过订单
        String seckillKey = RedisConst.SECKILL_STOCK_PREFIX + skuId;
        String rightPop = (String) redisTemplate.boundListOps(seckillKey).rightPop();
        if (StringUtils.isEmpty(rightPop)) {
            redisTemplate.convertAndSend("seckillpush", skuId + ":0");//售罄后要广播通知更新状态位
            return;
        }
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(findSeckillGoodsById(skuId));
        orderRecode.setNum(1);
        orderRecode.setOrderStr(MD5.encrypt(skuId + userId));
        String orderKey = RedisConst.SECKILL_ORDERS;
        redisTemplate.boundHashOps(orderKey).put(userId, orderRecode);//秒杀成功，记录进入缓存
        updateStockCount(skuId);
    }

    /*
     * 1.判断用户是否在缓存中存在(已经下了订单)
     *      1.1判断用户是否抢单成功
     * 2.判断用户是否从前下过订单
     * 3.判断状态位
     * */
    @Override
    public Result<Object> checkOrder(Long skuId, String userId) {
        Boolean hasKey = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (Boolean.TRUE.equals(hasKey)) {//用户已经下了订单
            String orderKey = RedisConst.SECKILL_ORDERS;
            OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(orderKey).get(userId);
            if (orderRecode != null) return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);//抢单成功！
        }
        String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
        if (orderId != null) return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);//从前下过订单,返回订单号
        String state = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(state)) return Result.build(null, ResultCodeEnum.SECKILL_FAIL);//售罄
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);//默认状态，排队中
    }

    /*
     * 更新缓存和数据库中的库存
     * */
    private void updateStockCount(Long skuId) {
        String seckillKey = RedisConst.SECKILL_STOCK_PREFIX + skuId;
        Long size = redisTemplate.boundListOps(seckillKey).size();
        if (size == null) return;
        if ((size & 1) == 0 || size == 1) { //偶数时更新，减小与数据库的交互,异步更新数据库和缓存
            SeckillGoods seckillGoods = findSeckillGoodsById(skuId);
            seckillGoods.setStockCount(size.intValue());
            seckillGoodsAsync.updateSeckillStockAsync(seckillGoodsMapper, seckillGoods);
            seckillGoodsAsync.updateSeckillStockRedisAsync(redisTemplate, seckillGoods, skuId);
        }
    }
}







