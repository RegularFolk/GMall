package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@FeignClient(value = "service-list", fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {
    String HOME_URL = "/api/list/";

    @ApiOperation("创建索引和映射")
    @GetMapping(HOME_URL + "inner/createIndex")
    Result<Object> createIndex();

    @ApiOperation("商品的上架")
    @GetMapping(HOME_URL + "inner/upperGoods/{skuId}")
    Result<Object> upperGoods(@PathVariable("skuId") Long skuId);

    @ApiOperation("商品的下架")
    @GetMapping(HOME_URL + "inner/lowerGoods/{skuId}")
    Result<Object> lowerGoods(@PathVariable("skuId") Long skuId);

    @ApiOperation("商品热度增加")
    @GetMapping(HOME_URL + "inner/incrHotScore/{skuId}")
    Result<Object> incrHotScore(@PathVariable("skuId") Long skuId);

    @ApiOperation("检索商品")
    @PostMapping("/api/list")
    Result list(@RequestBody SearchParam searchParam);
}
