package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> map = new HashMap<>();
        //使用异步编排
        CompletableFuture<SkuInfo> skuInfoCF = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        CompletableFuture<Void> categoryViewCF = skuInfoCF.thenAcceptAsync((skuInfo) -> {
            map.put("categoryView", productFeignClient.getCategoryView(skuInfo.getCategory3Id()));
        }, threadPoolExecutor);
        CompletableFuture<Void> spuSaleAttrListCF = skuInfoCF.thenAcceptAsync((skuInfo -> {
            map.put("spuSaleAttrList", productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId()));
        }), threadPoolExecutor);
        CompletableFuture<Void> valuesSkuJsonCF = skuInfoCF.thenAcceptAsync(skuInfo -> {
            map.put("valuesSkuJson", JSON.toJSONString(productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId())));
        }, threadPoolExecutor);
        CompletableFuture<Void> priceCF = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        }, threadPoolExecutor);
        CompletableFuture<Void> incrHotScoreCF = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);//每次获取当前sku说明当前sku热度上升了，需要加一
        //使用多任务进行组合
        CompletableFuture.allOf(
                skuInfoCF,
                categoryViewCF,
                spuSaleAttrListCF,
                valuesSkuJsonCF,
                priceCF,
                incrHotScoreCF).join();
        return map;
    }
}
