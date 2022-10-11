package com.atguigu.gmall.product.service;

public interface ManageAsyncService {
    void onSaleAsync(Long skuId);

    void cancelSaleAsync(Long skuId);
}
