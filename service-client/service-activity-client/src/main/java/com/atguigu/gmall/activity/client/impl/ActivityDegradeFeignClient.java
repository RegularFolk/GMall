package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component
public class ActivityDegradeFeignClient implements FallbackFactory<ActivityFeignClient> {
    @Override
    public ActivityFeignClient create(Throwable throwable) {
        return new ActivityFeignClient() {
            @Override
            public List<SeckillGoods> findAll() {
                printCause();
                return null;
            }

            @Override
            public SeckillGoods getSeckillGoods(Long skuId) {
                printCause();
                return null;
            }

            @Override
            public Result<Map<String, Object>> seckillTrade() {
                printCause();
                return null;
            }

            private void printCause() {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.println(throwable.getMessage());
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }
        };
    }
}
