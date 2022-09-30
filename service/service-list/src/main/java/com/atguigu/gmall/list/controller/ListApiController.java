package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {
    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    @ApiOperation("创建索引和映射")
    @GetMapping("inner/createIndex")
    public Result<Object> createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    @ApiOperation("商品的上架")
    @GetMapping("inner/upperGoods/{skuId}")
    public Result<Object> upperGoods(@PathVariable("skuId") Long skuId) {
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    @ApiOperation("商品的下架")
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result<Object> lowerGoods(@PathVariable("skuId") Long skuId) {
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    @ApiOperation("商品热度增加")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result<Object> incrHotScore(@PathVariable("skuId") Long skuId) {
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @ApiOperation("检索商品")
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam){
        return Result.ok(searchService.search(searchParam));
    }
}
