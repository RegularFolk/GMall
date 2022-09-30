package com.atguigu.gmall.order.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderDegradeFeignClient implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable throwable) {
        return new OrderFeignClient() {
            private void printCause() {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.println(throwable.getMessage());
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }

            @Override
            public Result<Map<String, Object>> trade() {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<Object> submitOrder(OrderInfo orderInfo) {
                printCause();
                return null;
            }
        };
    }
}
