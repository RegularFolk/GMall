package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.common.commonConfig.FeignLogConfig;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        value = "service-payment",
        fallbackFactory = PaymentDegradeFeignClient.class,
        configuration = FeignLogConfig.class)
@Component
public interface PaymentFeignClient {
    String HOME_URL = "/api/payment/alipay/";

    @RequestMapping(HOME_URL + "submit/{orderId}")
    String aliPay(@PathVariable("orderId") Long orderId);

    @GetMapping(HOME_URL + "callback/return")
    String callBackReturn();

    @PostMapping(HOME_URL + "callback/notify")
    String callBackNotify(@RequestParam Map<String, String> paramMap);

    @GetMapping(HOME_URL + "refund/{orderId}")
    Result<Object> refund(@PathVariable("orderId") Long orderId);

    //关闭支付宝交易记录
    @GetMapping(HOME_URL + "closePay/{orderId}")
    boolean closePay(@PathVariable("orderId") Long orderId);

    //查询支付宝是否有交易记录
    @GetMapping(HOME_URL + "checkPayment/{orderId}")
    boolean checkPayment(@PathVariable("orderId") Long orderId);

    //查询电商本地是否有交易记录
    @GetMapping(HOME_URL + "getPaymentInfo/{outTradeNo}")
    PaymentInfo getPaymentInfo(@PathVariable("outTradeNo") String outTradeNo);
}
