package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api("购物车控制器")
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartInfoService cartInfoService;

    @ApiOperation("添加购物车")
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result<Object> addToCart(@PathVariable("skuId") Long skuId,
                                    @PathVariable("skuNum") Integer skuNum,
                                    HttpServletRequest request) {
        String userId = getUserOrTempId(request);
        System.out.println(userId);
        cartInfoService.addToCart(skuId, Long.parseLong(userId), skuNum);
        return Result.ok();
    }

    //获取userId，没有则获取tempId
    private String getUserOrTempId(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {//未登录状态，需要获取在前端分配的临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        return userId;
    }

    @ApiOperation("查询购物车列表")
    @GetMapping("cartList")
    public Result<List<CartInfo>> cartList(HttpServletRequest request) {
        List<CartInfo> cartList = cartInfoService.getCartList(
                AuthContextHolder.getUserId(request),
                AuthContextHolder.getUserTempId(request));
        return Result.ok(cartList);
    }

    @ApiOperation("更新选中状态")
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result<Object> checkCart(@PathVariable("skuId") Long skuId,
                                    @PathVariable("isChecked") Integer isChecked,
                                    HttpServletRequest request) {
        String userOrTempId = getUserOrTempId(request);
        cartInfoService.checkCart(userOrTempId, isChecked, skuId);
        return Result.ok();
    }

    @ApiOperation("删除购物车的一项")
    @DeleteMapping("deleteCart/{skuId}")
    public Result<Object> deleteCart(@PathVariable("skuId") Long skuId,
                                     HttpServletRequest request) {
        String userOrTempId = getUserOrTempId(request);
        cartInfoService.deleteCart(skuId, userOrTempId);
        return Result.ok();
    }

    @ApiOperation("根据userId查询购物车列表")
    @GetMapping("getCartCheckedList/{userId}")
    public Result<List<CartInfo>> getCartCheckedList(@PathVariable("userId") String userId) {
        List<CartInfo> cartInfos = cartInfoService.getCartCheckedList(userId);
        return Result.ok(cartInfos);
    }

    @ApiOperation("重新加载缓存的购物车数据")
    @GetMapping("loadCartCache/{userId}")
    public Result<Object> loadCartCache(@PathVariable("userId") String userId) {
        cartInfoService.loadCartInfoCache(userId);
        return Result.ok();
    }
}
