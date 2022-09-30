package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface BaseTradeMarkService {

    List<BaseTrademark> getTrademarkList();

    IPage<BaseTrademark> selectPage(Page<BaseTrademark> page);

    BaseTrademark get(Long id);

    void save(BaseTrademark banner);

    void update(BaseTrademark banner);

    void remove(Long id);
}
