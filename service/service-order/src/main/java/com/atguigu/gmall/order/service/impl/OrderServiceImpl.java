package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderAsyncService;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

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

    @Autowired
    private RabbitService rabbitService;

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
        Long orderId = orderInfo.getId();
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderId(orderId);
            orderAsyncService.insertOrderDetailAsync(orderDetail);
        });//异步插入所有orderDetail
        //发送到rabbitMQ延时消息
        boolean isSent = rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderId, MqConst.DELAY_TIME);
        if (!isSent) throw new RuntimeException("超时消息发送失败！");
        return orderId;
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

    @Override//修改订单状态
    public void execExpireOrder(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        //发送一个消息关闭记录
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override//查询orderInfo加上orderDetail
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = getById(orderId);
        QueryWrapper<OrderDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("order_id", orderId);
        List<OrderDetail> detailList = orderDetailMapper.selectList(detailWrapper);
        if (orderInfo != null) orderInfo.setOrderDetailList(detailList);
        return orderInfo;
    }

    @Override//发送消息给库存，并修改订单状态
    public void sendOrderStatus(Long orderId) {
        //更改订单状态为已通知仓库
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        String wareJson = initWareOrder(orderId);
        //给库存系统发布消息，库存系统写好了监听消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    //发送减库存消息的字符串,由OrderInfo中部分字段组成
    private String initWareOrder(Long orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map<String, Object> map = getMapByOrderInfo(orderInfo);
        return JSON.toJSONString(map);
    }

    @Override//将orderInfo数据转为Map
    public Map<String, Object> getMapByOrderInfo(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());  // 仓库Id ，减库存拆单时需要使用！
        ArrayList<Map<String, Object>> detailList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        orderDetailList.forEach(orderDetail -> {  //orderDetail复合结构
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            detailMap.put("skuName", orderDetail.getSkuName());
            detailList.add(detailMap);
        });
        map.put("details", detailList);
        return map;
    }

    @Override//orderId对应原始订单，根据wareSkuMap拆成多个子订单，装入list返回
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        OrderInfo orderInfo = getOrderInfo(Long.parseLong(orderId));
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        List<OrderDetail> detailList = orderInfo.getOrderDetailList();
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(maps)) {
            maps.forEach(map -> {//map集合中的每一个map对应一个wareId
                String wareId = (String) map.get("wareId");
                List<String> skuIds = (List<String>) map.get("skuIds");
                Set<String> skuSet = new HashSet<>(skuIds);
                OrderInfo subOrderInfo = new OrderInfo();//子订单
                BeanUtils.copyProperties(orderInfo, subOrderInfo);
                subOrderInfo.sumTotalAmount();
                subOrderInfo.setParentOrderId(Long.parseLong(orderId));
                subOrderInfo.setId(null);
                subOrderInfo.setWareId(wareId);
                ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
                detailList.forEach(orderDetail -> {
                    if (skuSet.contains(orderDetail.getSkuId().toString())) {
                        orderDetailList.add(orderDetail);
                    }
                });
                subOrderInfo.setOrderDetailList(orderDetailList);
                saveOrderInfo(subOrderInfo);//异步加入,优化循环速度
                subOrderInfoList.add(subOrderInfo);
            });
        }
        return subOrderInfoList;
    }

    @Override
    public void execExpireOrder(Long orderId, boolean flag) {
        if (flag) execExpireOrder(orderId);
        else updateOrderStatus(orderId, ProcessStatus.CLOSED);
    }

}
