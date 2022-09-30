package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.item.service.ItemService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    @ApiOperation("获取sku详细信息")
    @GetMapping("{skuId}")
    public Map<String, Object> getItem(@PathVariable("skuId") Long skuId) {
        return itemService.getBySkuId(skuId);
    }
}
