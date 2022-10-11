package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
public class SeckillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @GetMapping("seckill.html")
    public String index(Model model) {
        List<SeckillGoods> seckillGoodsList = activityFeignClient.findAll();
        model.addAttribute("list", seckillGoodsList);
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable("skuId") Long skuId, Model model) {
        SeckillGoods seckillGoods = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item", seckillGoods);
        return "seckill/item";
    }

    //进入排队
    @GetMapping("seckill/queue.html")
    public String queue(HttpServletRequest request) {
        String skuId = request.getParameter("skuId");
        String skuIdStr = request.getParameter("skuIdStr");
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        Result<Map<String, Object>> result = activityFeignClient.seckillTrade();
        if (result.isOk()) {
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        } else {
            model.addAttribute("message", result.getMessage());
            return "seckill/fail";
        }
    }

}
