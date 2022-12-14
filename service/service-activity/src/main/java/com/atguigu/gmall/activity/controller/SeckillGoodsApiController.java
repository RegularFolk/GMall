package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("/findAll")
    public List<SeckillGoods> findAll() {
        return seckillGoodsService.findAll();
    }

    @GetMapping("/getSeckillGoods/{skuId}")
    public SeckillGoods getSeckillGoods(@PathVariable("skuId") Long skuId) {
        return seckillGoodsService.findSeckillGoodsById(skuId);
    }

    @ApiOperation("???????????????,?????????????????????????????????")
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result<Object> getSeckillSkuIdStr(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //????????????????????????:userId MD5???????????????????????????????????????
        SeckillGoods seckillGoods = seckillGoodsService.findSeckillGoodsById(skuId);
        String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId)) {
            Date curTime = new Date();
            if (curTime.compareTo(seckillGoods.getEndTime()) < 0 &&
                    curTime.compareTo(seckillGoods.getStartTime()) >= 0) {//???????????????????????????
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("????????????????????????");
    }

    @ApiOperation("??????")
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result<Object> seckillOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        String skuIdStr = request.getParameter("skuIdStr");
        String userId = AuthContextHolder.getUserId(request);
        if (!skuIdStr.equals(MD5.encrypt(userId))) return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);//????????????
        String state = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(state)) return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);//????????????
        else if ("0".equals(state)) return Result.build(null, ResultCodeEnum.SECKILL_FINISH);//?????????
        else {
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        }
        return Result.ok();
    }

    @ApiOperation("??????????????????")
    @GetMapping("auth/checkOrder/{skuId}")
    public Result<Object> checkOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }

    //??????????????????????????????????????????????????????
    @ApiOperation("??????????????????")
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> seckillTrade(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (orderRecode == null) return Result.fail(map).message("???????????????");
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        detailArrayList.add(orderDetail);
        map.put("detailArrayList", detailArrayList);
        map.put("userAddressList", userAddressList);
        map.put("totalNum", "1");
        map.put("totalAmount", seckillGoods.getCostPrice());
        return Result.ok(map);
    }

    @ApiOperation("??????????????????")
    @PostMapping("auth/submitOrder")
    public Result<Object> submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId = orderFeignClient.submitSeckillOrder(orderInfo);
        if (orderId == null) return Result.fail().message("?????????????????????");
        //????????????
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //??????????????????????????????????????????
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId, orderId.toString());
        return Result.ok(orderId);
    }


}







