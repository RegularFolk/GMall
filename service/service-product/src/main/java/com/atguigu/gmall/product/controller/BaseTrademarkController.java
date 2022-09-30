package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTradeMarkService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {
    @Autowired
    BaseTradeMarkService baseTradeMarkService;

    @GetMapping("getTrademarkList")
    public Result<List<BaseTrademark>> getTrademarkList() {
        return Result.ok(baseTradeMarkService.getTrademarkList());
    }

    @ApiOperation("分页列表")
    @GetMapping("{page}/{limit}")
    public Result<Object> index(@PathVariable("page") Long page,
                                @PathVariable("limit") Long limit) {
        Page<BaseTrademark> page1 = new Page<>(page, limit);
        return Result.ok(baseTradeMarkService.selectPage(page1));
    }

    @ApiOperation("根据商标id查询商标")
    @GetMapping("get/{id}")
    public Result<BaseTrademark> get(@PathVariable("id") Long id) {
        return Result.ok(baseTradeMarkService.get(id));
    }

    @ApiOperation("新增商标")
    @PostMapping("save")
    public Result<Object> save(@RequestBody BaseTrademark banner) {
        baseTradeMarkService.save(banner);
        return Result.ok();
    }

    @ApiOperation("修改商标")
    @PutMapping("update")
    public Result<Object> update(@RequestBody BaseTrademark banner) {
        baseTradeMarkService.update(banner);
        return Result.ok();
    }

    @ApiOperation("根据商标id删除商标")
    @DeleteMapping("remove/{id}")
    public Result<Object> remove(@PathVariable("id") Long id) {
        baseTradeMarkService.remove(id);
        return Result.ok();
    }
}
