package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    /*
     * 查询所有一级分类
     * */
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        return Result.ok(manageService.getCategory1());
    }

    /*
     * 根据一级分类id查询所有二级分类
     * */
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable("category1Id") Long category1Id) {
        return Result.ok(manageService.getCategory2(category1Id));
    }

    /*
     * 根据二级分类id查询所有三级分类
     * */
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable("category2Id") Long category2Id) {
        return Result.ok(manageService.getCategory3(category2Id));
    }

    /*
     * 根据分类id查询对应所有属性数据
     * */
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable("category1Id") Long category1Id,
                                                   @PathVariable("category2Id") Long category2Id,
                                                   @PathVariable("category3Id") Long category3Id) {
        return Result.ok(manageService.getAttrInfoList(category1Id, category2Id, category3Id));
    }

    @PostMapping("saveAttrInfo")
    public Result<Object> saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable("attrId") Long attrId) {
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        if (attrInfo != null) {
            return Result.ok(attrInfo.getAttrValueList());
        }
        return Result.ok();
    }

    @ApiOperation("分页查询Sku信息")
    @GetMapping("/list/{page}/{limit}")
    public Result<IPage<SkuInfo>> index(@PathVariable("page") Long page,
                                        @PathVariable("limit") Long limit) {
        Page<SkuInfo> page1 = new Page<>(page, limit);
        return Result.ok(manageService.getPage(page1));
    }

    @ApiOperation("商品上架")
    @GetMapping("onSale/{skuId}")
    public Result<Object> onSale(@PathVariable("skuId") Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    @ApiOperation("商品下架")
    @GetMapping("cancelSale/{skuId}")
    public Result<Object> cancelSale(@PathVariable("skuId") Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }

}
