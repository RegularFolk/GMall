package com.atguigu.gmall.product.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(
        value = "service-product",
        fallbackFactory = ProductDegradeFeignClient.class,
        configuration = FeignLogConfig.class
)
@Component
public interface ProductFeignClient {
    String HOME_URL = "/api/product/";

    @GetMapping(HOME_URL + "inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId);

    @GetMapping(HOME_URL + "inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    @GetMapping(HOME_URL + "inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable("skuId") Long skuId);

    @GetMapping(HOME_URL + "inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                   @PathVariable("spuId") Long spuId);

    @GetMapping(HOME_URL + "inner/getSkuValueIdsMap/{spuId}")
    Map<Object, Object> getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    @ApiOperation("获取JSON格式的全部分类信息")
    @GetMapping(HOME_URL + "getBaseCategoryList")
    Result<List<JSONObject>> getBaseCategoryList();

    @ApiOperation("通过品牌id查询品牌")
    @GetMapping(HOME_URL + "inner/getTrademark/{tmId}")
    Result<BaseTrademark> getTradeMark(@PathVariable("tmId") Long tmId);

    @ApiOperation("通过skuId查询baseAttrValue")
    @GetMapping(HOME_URL + "inner/getAttrList/{skuId}")
    Result<List<BaseAttrInfo>> getAttrList(@PathVariable("skuId") Long skuId);
}
