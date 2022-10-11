package com.atguigu.gmall.payment.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentDegradeFeignClient implements FallbackFactory<PaymentFeignClient> {
    @Override
    public PaymentFeignClient create(Throwable throwable) {
        return new PaymentFeignClient() {
            private void printCause() {
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.println(throwable.getMessage());
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            }

            @Override
            public String aliPay(Long orderId) {
                printCause();
                return null;
            }

            @Override
            public String callBackReturn() {
                printCause();
                return null;
            }

            @Override
            public String callBackNotify(Map<String, String> paramMap) {
                printCause();
                return null;
            }

            @Override
            public Result<Object> refund(Long orderId) {
                printCause();
                return null;
            }

            @Override
            public boolean closePay(Long orderId) {
                printCause();
                return false;
            }

            @Override
            public boolean checkPayment(Long orderId) {
                printCause();
                return false;
            }

            @Override
            public PaymentInfo getPaymentInfo(String outTradeNo) {
                printCause();
                return null;
            }
        };
    }
}
