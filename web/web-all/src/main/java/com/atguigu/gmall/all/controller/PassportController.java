package com.atguigu.gmall.all.controller;

import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Api("用户认证接口")
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);//设置原来的URL，用于登录完成后回跳
        return "login";
    }

}
