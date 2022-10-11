package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@FeignClient(
        value = "service-order",
        fallbackFactory = OrderDegradeFeignClient.class,
        configuration = FeignLogConfig.class
)
public interface OrderFeignClient {
    String HOME_URL = "/api/order/";

    @ApiOperation("确认订单,即获取订单")
    @GetMapping(HOME_URL + "auth/trade")
    Result<Map<String, Object>> trade();


    @ApiOperation("提交订单")
    @PostMapping(HOME_URL + "auth/submitOrder")
    Result<Object> submitOrder(@RequestBody OrderInfo orderInfo);

    @ApiOperation("回显订单和订单详细信息")
    @GetMapping(HOME_URL + "inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable("orderId") Long orderId);

    @ApiOperation("提交参与秒杀的订单接口")
    @PostMapping(HOME_URL + "inner/seckill/submitOrder")
    Long submitSeckillOrder(@RequestBody OrderInfo orderInfo);
}
