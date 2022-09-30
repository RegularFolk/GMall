package com.atguigu.gmall.user.client;

import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        value = "service-user",
        fallbackFactory = UserDegradeFeignClient.class,
        configuration = FeignLogConfig.class
)
@Component
public interface UserFeignClient {
    String HOME_URL = "/api/user/";

    @ApiOperation("获取用户地址")
    @GetMapping(HOME_URL + "inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable("userId") String userId);
}
