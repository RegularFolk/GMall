package com.atguigu.gmall.product.client.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ProductDegradeFeignClient implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable throwable) {
        return new ProductFeignClient() {
            private void printCause() {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.println(throwable.getMessage());
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }

            @Override
            public SkuInfo getSkuInfo(Long skuId) {
                printCause();
                return null;
            }

            @Override
            public BaseCategoryView getCategoryView(Long category3Id) {
                printCause();
                return null;
            }

            @Override
            public BigDecimal getSkuPrice(Long skuId) {
                printCause();
                return null;
            }

            @Override
            public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
                printCause();
                return null;
            }

            @Override
            public Map<Object, Object> getSkuValueIdsMap(Long spuId) {
                printCause();
                return null;
            }

            @Override
            public Result<List<JSONObject>> getBaseCategoryList() {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<BaseTrademark> getTradeMark(Long tmId) {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<List<BaseAttrInfo>> getAttrList(Long skuId) {
                printCause();
                return Result.fail();
            }
        };
    }
}
