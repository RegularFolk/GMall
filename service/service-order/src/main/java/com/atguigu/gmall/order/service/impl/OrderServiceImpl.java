package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderAsyncService;
import com.atguigu.gmall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Value("${ware.url}")
    private String wareUrl;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderAsyncService orderAsyncService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    @Transactional//TODO 使用了异步，这个事务实际上是不起作用的
    public Long saveOrderInfo(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();//↓ 设置orderInfo的基本属性
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());//一个具体的日期，而不是一段时间
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        if (StringUtils.isEmpty(orderInfo.getPaymentWay())) orderInfo.setPaymentWay("ONLINE");//默认设置支付方式为在线支付
        //获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder tradeBody = new StringBuilder();
        orderDetailList.forEach(orderDetail -> tradeBody.append(orderDetail.getSkuName()).append(" "));
        orderInfo.setTradeBody(tradeBody.substring(0, Math.min(100, tradeBody.length())));
        orderInfoMapper.insert(orderInfo);
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderId(orderInfo.getId());
            orderAsyncService.insertOrderDetailAsync(orderDetail);
        });//异步插入所有orderDetail
        return orderInfo.getId();
    }

    @Override//检查缓存中是否存在指定的流水号,防止用户通过网页回退从而重复提交
    public boolean checkTradeCode(String userId, String tradeNo) {
        String tradeNoKey = getTradeNoKey(userId);
        String cacheValue = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(cacheValue);
    }

    private String getTradeNoKey(String userId) {
        return "user:" + userId + ":tradeCode";
    }

    @Override//获取订单流水号的同时加入缓存
    public String getTradeNo(String userId) {
        String tradeNoKey = getTradeNoKey(userId);
        String cacheNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        if (!StringUtils.isEmpty(cacheNo)) return cacheNo;//缓存已存在则直接返回
        cacheNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tradeNoKey, cacheNo);
        return cacheNo;
    }

    @Override//判断skuId对应商品的库存是否大于skuNum
    public boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);//0代表没有，1代表有
    }

}
