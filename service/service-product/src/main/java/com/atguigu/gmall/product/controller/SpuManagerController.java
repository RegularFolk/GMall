package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("admin/product")
public class SpuManagerController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("分页获取销售属性")
    @GetMapping("{page}/{limit}")
    public Result<Object> getSpuInfoList(@PathVariable("page") Long page,
                                         @PathVariable("limit") Long limit,
                                         SpuInfo spuInfo) {
        Page<SpuInfo> spuInfoPage = new Page<>(page, limit);
        IPage<SpuInfo> spuInfoIPage = manageService.getSpuInfoPage(spuInfoPage, spuInfo);
        return Result.ok(spuInfoIPage);
    }

    @ApiOperation("获取全部销售属性")
    @GetMapping("baseSaleAttrList")
    public Result<List<BaseSaleAttr>> baseSaleAttrList() {
        return Result.ok(manageService.getBaseSaleAttrList());
    }

    @ApiOperation("保存SpuInfo")
    @PostMapping("saveSpuInfo")
    public Result<Object> saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
