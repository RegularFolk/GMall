package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


@Component//全局过滤器，过滤器中编写业务逻辑，规定访问特点URL时必须登录
public class AuthGlobalFilter implements GlobalFilter {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")//从配置文件中获取到的控制器
    private String authUrlsUrl;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取用户访问的URL
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();
        //判断path类型
        if (antPathMatcher.match("/**/inner/**", path)) {//属于内部接口 /**/inner/**  不允许访问
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //通过userId判断用户是否处于登录状态
        String userId = getUserId(request);
        String userTempId = getUserTempId(request);
        if ("-1".equals(userId)) {//ip不一致，发生盗用
            return out(response, ResultCodeEnum.PERMISSION);
        }
        if (antPathMatcher.match("/api/**/auth/**", path)) {//特定的需要登录的api
            if (StringUtils.isEmpty(userId)) {
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        String[] split = authUrlsUrl.split("[,]");
        for (String s : split) {
            if (path.contains(s) && StringUtils.isEmpty(userId)) {//属于需要登录的控制器，跳转到登录页面
                response.setStatusCode(HttpStatus.SEE_OTHER);//303状态码，表示资源需要重定向
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());
                return response.setComplete();
            }
        }
        //全部验证通过:将userId传递给后台微服务
        //gateway消费了一次请求后，就不能再向下一个请求传播，原先请求的header会失效，所以需要重新按照原先的装配一下userId
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            ServerHttpRequest mutatedRequest = null;
            if (!StringUtils.isEmpty(userId)) {
                mutatedRequest = request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)) {
                mutatedRequest = request.mutate().header("userTempId", userTempId).build();
            }
            //为什么不用 request.getHeaders().set("userId", userId) ?
            return chain.filter(
                    exchange.mutate().request(
                            mutatedRequest
                    ).build()
            );
        }
        return chain.filter(exchange);
    }

    //获取临时用户id，先从cookie里面获取，再从请求头中获取
    private String getUserTempId(ServerHttpRequest request) {
        HttpCookie userTempId = request.getCookies().getFirst("userTempId");
        if (userTempId != null) return userTempId.getValue();
        else {
            List<String> list = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(list)) return list.get(0);
        }
        return "";
    }

    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        Result<Object> result = Result.build(null, resultCodeEnum);
        String strJson = JSON.toJSONString(result);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        DataBuffer dataBuffer = response.bufferFactory().wrap(strJson.getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }

    //通过redis获取用户id
    private String getUserId(ServerHttpRequest request) {
        String token;
        List<String> list = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(list)) token = list.get(0);
        else {
            HttpCookie cookie = request.getCookies().getFirst("token");
            token = cookie == null ? "" : cookie.getValue();
        }
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        Object o = redisTemplate.opsForValue().get(userKey);
        String text = (String) o;
        JSONObject jsonObject = JSONObject.parseObject(text, JSONObject.class);
        if (jsonObject == null) {
            return "";
        }
        //校验
        String curIp = IpUtil.getGatwayIpAddress(request);
        if (!curIp.equals(jsonObject.get("ip"))) {
            return "-1";
        }
        return (String) jsonObject.get("userId");
    }

//    @Test
//    public void x(){
//        JSONObject jsonObject = JSONObject.parseObject("{\"ip\":\"192.168.200.1\",\"userId\":\"3\"}", JSONObject.class);
//        System.out.println(jsonObject.toJSONString());
//    }
}
