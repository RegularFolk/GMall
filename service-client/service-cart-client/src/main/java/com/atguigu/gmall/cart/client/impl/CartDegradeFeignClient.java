package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
public class CartDegradeFeignClient implements FallbackFactory<CartFeignClient> {
    @Override
    public CartFeignClient create(Throwable throwable) {
        return new CartFeignClient() {
            private void printCause() {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.println(throwable.getMessage());
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }

            @Override
            public Result<Object> addToCart(Long skuId, Integer skuNum) {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<List<CartInfo>> cartList() {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<Object> checkCart(Long skuId, Integer isChecked) {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<Object> deleteCart(Long skuId) {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<List<CartInfo>> getCartCheckedList(String userId) {
                printCause();
                return Result.fail();
            }

            @Override
            public Result<Object> loadCartCache(String userId) {
                printCause();
                return null;
            }
        };
    }
}
