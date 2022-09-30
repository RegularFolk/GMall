package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @ApiOperation("查看购物车")
    @RequestMapping("cart.html")
    public String index() {
        return "cart/index";
    }

    @ApiOperation("添加购物车")
    @RequestMapping("addCart.html")
    private String addCart(@RequestParam(name = "skuId") Long skuId,
                           @RequestParam(name = "skuNum") Integer skuNum,
                           HttpServletRequest request) {
        cartFeignClient.addToCart(skuId, skuNum);
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);
        return "cart/addCart";
    }


}
