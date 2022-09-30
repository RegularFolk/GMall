package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/product")
public class ProductApiController {
    @Autowired
    private ManageService manageService;

    @ApiOperation("根据skuId获取带着image的skuInfo")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId) {
        return manageService.getSkuInfo(skuId);
    }

    @ApiOperation("根据三级分类id查询全部分类信息")
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id) {
        return manageService.getCategoryViewByCategory3Id(category3Id);
    }

    @ApiOperation("根据skuId获取sku价格")
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable("skuId") Long skuId) {
        return manageService.getSkuPrice(skuId);
    }

    @ApiOperation("根据skuId和spuId查询销售属性集合")
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,
                                                          @PathVariable("spuId") Long spuId) {
        return manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @ApiOperation("根据spuId查询其所有sku以及对应销售属性，以map返回")
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map<Object, Object> getSkuValueIdsMap(@PathVariable("spuId") Long spuId) {
        return manageService.getSkuValueIdsMap(spuId);
    }

    @ApiOperation("获取JSON格式的全部分类信息")
    @GetMapping("getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        return Result.ok(manageService.getBaseCategoryList());
    }


    @ApiOperation("通过品牌id查询品牌")
    @GetMapping("inner/getTrademark/{tmId}")
    public Result<BaseTrademark> getTradeMark(@PathVariable("tmId") Long tmId) {
        BaseTrademark trademark = manageService.getTradeMarkByTmId(tmId);
        return Result.ok(trademark);
    }

    @ApiOperation("通过skuId查询baseAttrValue")
    @GetMapping("inner/getAttrList/{skuId}")
    public Result<List<BaseAttrInfo>> getAttrList(@PathVariable("skuId") Long skuId) {
        List<BaseAttrInfo> attrInfos = manageService.getAttrList(skuId);
        return Result.ok(attrInfos);
    }
}
