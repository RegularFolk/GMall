package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api("用户认证接口")
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @ApiOperation("登录")
    @PostMapping("login")//首次登录，不包括之后的用户认证
    public Result<Object> login(@RequestBody UserInfo userInfo, HttpServletRequest request) {
        userInfo = userService.login(userInfo);
        if (userInfo != null) {
            String token = UUID.randomUUID().toString().replaceAll("[-]", "");
            HashMap<String, Object> map = new HashMap<>();
            map.put("nickName", userInfo.getNickName());
            map.put("token", token);
            JSONObject userJson = new JSONObject();
            userJson.put("userId", userInfo.getId().toString());
            userJson.put("ip", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(
                    RedisConst.USER_LOGIN_KEY_PREFIX + token,
                    userJson.toJSONString(),
                    RedisConst.USERKEY_TIMEOUT,
                    TimeUnit.SECONDS
            );
            return Result.ok(map);
        } else {
            return Result.fail().message("登录失败！\n用户名或者密码错误！");
        }
    }

    @ApiOperation("退出登录")
    @GetMapping("logout")
    public Result<Object> logout(HttpServletRequest request) {
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }


}
