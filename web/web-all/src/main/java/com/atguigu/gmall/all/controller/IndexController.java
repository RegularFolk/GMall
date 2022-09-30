package com.atguigu.gmall.all.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller//主页相关
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @ApiOperation("以JSON格式获取所有分类信息")
    @RequestMapping({"/", "index.html"})
    public String index(HttpServletRequest request) {
        Result<List<JSONObject>> list = productFeignClient.getBaseCategoryList();
        request.setAttribute("list", list.getData());
        return "index/index";
    }
}
