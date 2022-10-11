package com.atguigu.gmall.activity.reveiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @SneakyThrows(IOException.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))//监听消息将秒杀消息存入缓存
    public void importToRedis(Message message, Channel channel) {
        //查询当天的秒杀商品集合，查询条件:当天、审核状态、剩余库存数
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        String curTime = DateUtil.formatDate(new Date());
        wrapper.eq("status", "1")  //状态为1
                .gt("stock_count", 0) //库存大于0
                .le("DATE_FORMAT(start_time,'%Y-%m-%d')", curTime)
                .ge("DATE_FORMAT(end_time,'%Y-%m-%d')", curTime);
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(seckillGoodsList)) {
            List<CompletableFuture<Void>> futureList = new ArrayList<>();
            seckillGoodsList.forEach(seckillGoods -> {
                Long skuId = seckillGoods.getSkuId();
                Boolean hasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(skuId.toString());
                if (!Boolean.TRUE.equals(hasKey)) {//该秒杀商品在缓存中之前不存在
                    CompletableFuture<Void> secGoodsCF = CompletableFuture.runAsync(() -> {//并行加入缓存
                        //加入缓存
                        redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(), seckillGoods);
                        //防止超卖,对每一个商品保存，如有10个goods就存十条goods记录，同样并行加入
                        List<CompletableFuture<Void>> stockList = new ArrayList<>();
                        for (int i = 0; i < seckillGoods.getStockCount(); i++) {
                            CompletableFuture<Void> stockCF = CompletableFuture.runAsync(() -> {
                                String key = RedisConst.SECKILL_STOCK_PREFIX + skuId;
                                redisTemplate.opsForList().leftPush(key, skuId.toString());
                            }, threadPoolExecutor);
                            stockList.add(stockCF);
                        }
                        //状态位初始化为1
                        redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + ":1");
                        if (!CollectionUtils.isEmpty(stockList)) {
                            CompletableFuture.allOf(stockList.toArray(new CompletableFuture[0])).join();//双重线程梅开二度
                        }
                    }, threadPoolExecutor);
                    futureList.add(secGoodsCF);
                }
            });
            if (!CollectionUtils.isEmpty(futureList)) {
                CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
            }
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @SneakyThrows(IOException.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))//监听时间
    public void seckill(UserRecode userRecode, Message message, Channel channel) {
        if (userRecode != null) {
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @SneakyThrows(IOException.class)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))//查询过期的秒杀活动
    public void clearSeckill(Message message, Channel channel) {
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.le("end_time", new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        List<CompletableFuture<Void>> cfList = new ArrayList<>();
        seckillGoodsList.forEach(seckillGoods -> {//并发删除所有过期秒杀商品
            CompletableFuture<Void> redisCF = CompletableFuture.runAsync(() -> {
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).delete(seckillGoods.getSkuId());
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId().toString());//删除库存
            });
            cfList.add(redisCF);
        });
        CompletableFuture.allOf(cfList.toArray(new CompletableFuture[0])).join();
        Long size = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).size();
        if (size != null && size == 0) redisTemplate.delete(RedisConst.SECKILL_GOODS);//秒杀商品为空时删除该key
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);//删除临时订单
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);//删除确认后的订单
        SeckillGoods seckillGoods = new SeckillGoods();//修改秒杀数据库记录
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods, wrapper);//查询条件也可作为修改条件
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
