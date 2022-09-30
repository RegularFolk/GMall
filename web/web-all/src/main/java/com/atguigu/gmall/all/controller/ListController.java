package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    @RequestMapping("list.html")
    public String listPage(SearchParam searchParam, Model model) {
        model.addAttribute("searchParam", searchParam);
        Result<Map> responseVo = listFeignClient.list(searchParam);
        model.addAllAttributes(responseVo.getData());
        //处理URL
        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);
        //品牌面包屑，当做一个字符串处理
        String trademarkParam = makeTrademarkParam(searchParam.getTrademark());
        model.addAttribute("trademarkParam", trademarkParam);
        //平台属性面包屑
        List<Map<String, String>> propsParamList = makeProps(searchParam.getProps());
        model.addAttribute("propsParamList", propsParamList);
        //orderMap用于排序
        Map<String, Object> orderMap = dealOrder(searchParam.getOrder());
        model.addAttribute("orderMap", orderMap);
        return "list/index";
    }

    private Map<String, Object> dealOrder(String order) {
        Map<String, Object> map = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split("[:]");
            if (split.length == 2) {
                map.put("type", split[0]);
                map.put("sort", split[1]);
            }
        } else {
            map.put("type", "1");
            map.put("sort", "asc");
        }
        return map;
    }

    private List<Map<String, String>> makeProps(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                if (!StringUtils.isEmpty(prop)) {
                    String[] split = prop.split("[:]");
                    if (split.length == 3) {
                        Map<String, String> map = new HashMap<>();
                        map.put("attrId", split[0]);
                        map.put("attrValue", split[1]);
                        map.put("attrName", split[2]);
                        list.add(map);
                    }
                }
            }
        }
        return list;
    }

    //获取品牌面包屑
    private String makeTrademarkParam(String trademark) {
        StringBuilder builder = new StringBuilder();
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split("[:]");
            if (split.length == 2) {
                builder.append("品牌:").append(split[1]);
            }
        }
        return builder.toString();
    }

    //获取请求路径中的请求参数,通过请求对象拼出URL
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder builder = new StringBuilder();
        //判断用户是否通过关键字检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            builder.append("keyword=").append(searchParam.getKeyword());
        }
        //判断用户是否通过分类id检索
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            builder.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            builder.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            builder.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //判断是否通过品牌检索
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            if (builder.length() > 0)
                builder.append("&trademark=").append(trademark);
        }
        //判断用户是否通过平台属性进行检索
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                if (builder.length() > 0)
                    builder.append("&props=").append(prop);
            }
        }
        return "list.html?" + builder;
    }

}
