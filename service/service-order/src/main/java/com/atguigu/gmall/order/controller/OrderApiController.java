package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderAsyncService;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private OrderAsyncService orderAsyncService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @ApiOperation("确认订单,即获取订单")
    @GetMapping("auth/trade")//带auth的链接是已经判断过处于已登录状态的
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        try {//对地址和订单的查询异步进行
            String userId = AuthContextHolder.getUserId(request);
            Future<List<UserAddress>> addressFuture = orderAsyncService.findUserAddressListByUserIdAsync(userId);
            Future<List<CartInfo>> checkedListFuture = orderAsyncService.getCartCheckedListAsync(userId);
            List<OrderDetail> orderDetails = new ArrayList<>();
            List<CartInfo> cartInfoList = checkedListFuture.get();
            cartInfoList.forEach(cartInfo -> {//将cartInfo装配进orderDetail
                OrderDetail orderDetail = packOrderDetail(cartInfo);
                orderDetails.add(orderDetail);
            });
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderDetailList(orderDetails);
            orderInfo.sumTotalAmount();//计算总金额
            String tradeNo = orderService.getTradeNo(userId);//获取订单流水号
            Map<String, Object> map = new HashMap<>();
            List<UserAddress> userAddresses = addressFuture.get();
            map.put("userAddressList", userAddresses);
            map.put("detailArrayList", orderDetails);
            map.put("totalNum", orderDetails.size());//一共有几种商品
            map.put("totalAmount", orderInfo.getTotalAmount());
            map.put("tradeNo", tradeNo);
            return Result.ok(map);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取订单失败！");
        }
    }

    private OrderDetail packOrderDetail(CartInfo cartInfo) {
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(cartInfo.getSkuId());
        orderDetail.setSkuName(cartInfo.getSkuName());
        orderDetail.setImgUrl(cartInfo.getImgUrl());
        orderDetail.setSkuNum(cartInfo.getSkuNum());
        orderDetail.setOrderPrice(cartInfo.getSkuPrice());
        return orderDetail;
    }

    @ApiOperation("提交订单")
    @PostMapping("auth/submitOrder")//提交前检查是否已被提交过
    public Result<Object> submitOrder(@RequestBody OrderInfo orderInfo,
                                      HttpServletRequest request) {
        if (orderInfo == null || orderInfo.getOrderDetailList() == null) return Result.fail().message("订单为空！");
        String userId = AuthContextHolder.getUserId(request);
        String tradeNo = request.getParameter("tradeNo");
        if (!orderService.checkTradeCode(userId, tradeNo)) return Result.fail().message("订单已被提交");
        List<String> errorList = new Vector<>();//并发加入集合，应该用vector
        List<CompletableFuture<Void>> futureList = new Vector<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        orderDetailList.forEach(orderDetail -> {
            CompletableFuture<Void> priceStockCF = CompletableFuture.runAsync(() -> {
                CompletableFuture<Void> stockCF = CompletableFuture.runAsync(() -> {
                    boolean checkStock = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!checkStock) errorList.add(orderDetail.getSkuId() + "库存不足！");
                }, threadPoolExecutor);
                CompletableFuture<Void> priceCF = CompletableFuture.runAsync(() -> {
                    BigDecimal latestPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    if (latestPrice.compareTo(orderDetail.getOrderPrice()) != 0) {//价格发生变动，查询新价格
                        cartFeignClient.loadCartCache(userId);
                        errorList.add(orderDetail.getSkuName() + "价格发生变动！");
                    }
                }, threadPoolExecutor);
                CompletableFuture.allOf(stockCF, priceCF).join();
            }, threadPoolExecutor);
            futureList.add(priceStockCF);
        });//双重线程，每一个商品的检查并行，商品间的库存和价格检查也并行
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        if (errorList.size() > 0) return Result.fail().message("库存不足或者价格发生变化！");
        orderAsyncService.deleteTradeNo(userId);//提交成功，异步删除缓存
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    @ApiOperation("回显订单和订单详细信息")
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    @ApiOperation("拆单(用于订单系统调用)")
    @PostMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");//key为wareId value为skuId的map的集合
        List<OrderInfo> orderInfos = orderService.orderSplit(orderId, wareSkuMap);
        List<Map<String, Object>> maps = new ArrayList<>();
        orderInfos.forEach(orderInfo -> {
            Map<String, Object> map = orderService.getMapByOrderInfo(orderInfo);
            maps.add(map);
        });
        return JSON.toJSONString(maps);
    }

    @ApiOperation("提交参与秒杀的订单接口")
    @PostMapping("inner/seckill/submitOrder")
    public Long submitSeckillOrder(@RequestBody OrderInfo orderInfo) {
        //相比于正常的提交，省略了库存和价格的验证
        return orderService.saveOrderInfo(orderInfo);
    }

}
