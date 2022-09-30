package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(
        value = "service-cart",
        fallbackFactory = CartDegradeFeignClient.class,
        configuration = FeignLogConfig.class
)
public interface CartFeignClient {
    String HOME_URL = "/api/cart/";

    //feign上不用写request，在web-util中已经处理了

    @ApiOperation("添加购物车")
    @PostMapping(HOME_URL + "addToCart/{skuId}/{skuNum}")
    Result<Object> addToCart(@PathVariable("skuId") Long skuId,
                             @PathVariable("skuNum") Integer skuNum);

    @ApiOperation("查询购物车列表")
    @GetMapping(HOME_URL + "cartList")
    Result<List<CartInfo>> cartList();

    @ApiOperation("更新选中状态")
    @GetMapping(HOME_URL + "checkCart/{skuId}/{isChecked}")
    Result<Object> checkCart(@PathVariable("skuId") Long skuId,
                             @PathVariable("isChecked") Integer isChecked);

    @ApiOperation("删除购物车的一项")
    @DeleteMapping(HOME_URL + "deleteCart/{skuId}")
    Result<Object> deleteCart(@PathVariable("skuId") Long skuId);

    @ApiOperation("根据userId查询购物车列表")
    @GetMapping(HOME_URL + "getCartCheckedList/{userId}")
    Result<List<CartInfo>> getCartCheckedList(@PathVariable("userId") String userId);

    @ApiOperation("重新加载缓存的购物车数据")
    @GetMapping(HOME_URL + "loadCartCache/{userId}")
    Result<Object> loadCartCache(@PathVariable("userId") String userId);
}
