package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("商品SKU接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("根据spuId查询spuImageList")
    @GetMapping("spuImageList/{spuId}")
    public Result<List<SpuImage>> spuImageList(@PathVariable("spuId") Long spuId) {
        return Result.ok(manageService.spuImageList(spuId));
    }

    @ApiOperation("根据spuId查询spuAttr")
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result<List<SpuSaleAttr>> spuSaleAttrList(@PathVariable("spuId") Long spuId) {
        return Result.ok(manageService.spuSaleAttrList(spuId));
    }

    @ApiOperation("添加SkuInfo")
    @PostMapping("saveSkuInfo")
    public Result<Object> saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }
}
