package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(
        value = "service-activity",
        fallbackFactory = ActivityDegradeFeignClient.class,
        configuration = FeignLogConfig.class
)
public interface ActivityFeignClient {

    String HONE_URL = "/api/activity/seckill/";

    @GetMapping(HONE_URL + "/findAll")
    List<SeckillGoods> findAll();

    @GetMapping(HONE_URL + "/getSeckillGoods/{skuId}")
    SeckillGoods getSeckillGoods(@PathVariable("skuId") Long skuId);

    @GetMapping(HONE_URL + "auth/trade")
    Result<Map<String, Object>> seckillTrade();
}
