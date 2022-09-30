package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTradeMarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseTradeMarkServiceImpl implements BaseTradeMarkService {

    @Autowired
    BaseTrademarkMapper baseTrademarkMapper;


    @Override
    public List<BaseTrademark> getTrademarkList() {
        return baseTrademarkMapper.selectList(null);
    }

    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> page) {
        QueryWrapper<BaseTrademark> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("id");
        return baseTrademarkMapper.selectPage(page, wrapper);
    }

    @Override
    public BaseTrademark get(Long id) {
        return baseTrademarkMapper.selectById(id);
    }

    @Override
    public void save(BaseTrademark banner) {
        baseTrademarkMapper.insert(banner);
    }

    @Override
    public void update(BaseTrademark banner) {
        baseTrademarkMapper.updateById(banner);
    }

    @Override
    public void remove(Long id) {
        baseTrademarkMapper.deleteById(id);
    }
}
