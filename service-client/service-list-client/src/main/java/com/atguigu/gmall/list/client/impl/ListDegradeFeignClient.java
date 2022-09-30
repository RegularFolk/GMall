package com.atguigu.gmall.list.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.stereotype.Component;

@Component
public class ListDegradeFeignClient  implements ListFeignClient {
    @Override
    public Result<Object> createIndex() {
        return Result.fail();
    }

    @Override
    public Result<Object> upperGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result<Object> lowerGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result<Object> incrHotScore(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result list(SearchParam searchParam) {
        return Result.fail();
    }
}
