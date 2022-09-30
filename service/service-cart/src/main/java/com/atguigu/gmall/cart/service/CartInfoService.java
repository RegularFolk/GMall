package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartInfoService {

    void addToCart(Long skuId, Long userId, Integer skuNum);

    List<CartInfo> getCartList(String userId, String userTempId);

    void checkCart(String userId, Integer isChecked, Long skuId);

    void deleteCart(Long skuId, String userId);

    List<CartInfo> getCartCheckedList(String userId);

    void loadCartInfoCache(String userId);
}
