package com.atguigu.gmall.item.client;

import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(value = "service-item", fallback = ItemDegradeFeignClient.class)
@Component
public interface ItemFeignClient {
    String HOME_URL = "/api/item/";

    @ApiOperation("获取sku详细信息")
    @GetMapping(HOME_URL + "{skuId}")
    Map<String, Object> getItem(@PathVariable("skuId") Long skuId);
}
