package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.item.client.ItemFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller//负责转到页面，所以不用RestController，返回值作为视图名称
public class ItemController {
    @Autowired
    private ItemFeignClient itemFeignClient;

    @ApiOperation("获取sku详情页面")
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable("skuId") Long skuId, Model model) {
        Map<String, Object> item = itemFeignClient.getItem(skuId);
        model.addAllAttributes(item);
//        System.out.println(">>>>>>>>>>>>>>>>>>>>");
//        item.forEach((k, v) -> {
//            System.out.print("k = " + k);
//            System.out.println("\tv = " + v);
//        });
//        System.out.println("<<<<<<<<<<<<<<<<<<<<");
        return "item/index";
    }


}
