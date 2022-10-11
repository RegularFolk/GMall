package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @RequestMapping("submit/{orderId}")
    @ResponseBody //1.数据是JSON  2.直接将数据输出到页面  这里返回的pageContent是一大串html，可以直接显示成页面
    public String aliPay(@PathVariable("orderId") Long orderId) {
        String pageContent = null;
        try {
            pageContent = alipayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return pageContent;
    }

    @GetMapping("callback/return")
    public String callBackReturn() {//同步回调，重定向到支付成功页面
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //异步回调，对商家进行支付成功的通知
    //  http://xsiv7k.natappfree.cc/api/payment/alipay/callback/notify   启动内网穿透工具
    @SneakyThrows(AlipayApiException.class)
    @PostMapping("callback/notify")
    @ResponseBody
    public String callBackNotify(@RequestParam Map<String, String> paramMap) {
        boolean checkV1 = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        if (checkV1) {
            //验签成功后，按照支付结果异步通知中的描述对支付结果中的业务进行二次校验
            String outTradeNo = paramMap.get("out_trade_no");//如果通过这outTradeNo可以获取数据，则说明验证成功
            String total_amount = paramMap.get("total_amount");
            String appId = paramMap.get("app_id");
            String tradeStatus = paramMap.get("trade_status");
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
            if (paymentInfo == null ||
                    new BigDecimal("0.01").compareTo(new BigDecimal(total_amount)) != 0 ||
                    !AlipayConfig.app_id.equals(appId)
            )
                return "failure";
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                //防止万一 当交易状态是支付完成或者交易结束时，支付状态是CLOSED或者是PAID，则返回failure
                if ("PAID".equals(paymentInfo.getPaymentStatus()) || "CLOSED".equals(paymentInfo.getPaymentStatus())) {
                    return "failure";
                }
                //更新记录表状态
                paymentService.paySuccess(outTradeNo, paramMap, PaymentType.ALIPAY.name());
                return "success";
            }
        }
        return "failure";
    }

    //申请退款
    @GetMapping("refund/{orderId}")
    public Result<Object> refund(@PathVariable("orderId") Long orderId) {
        boolean flag = alipayService.refund(orderId);
        if (flag) return Result.ok();
        return Result.fail().message("退款失败！");
    }

    @GetMapping("closePay/{orderId}")
    @ResponseBody//扫码之后支付宝端会生成支付记录，扫码后不支付通过该方法主动关闭交易
    public boolean closePay(@PathVariable("orderId") Long orderId) {
        return alipayService.closePay(orderId);
    }

    @GetMapping("checkPayment/{orderId}")
    @ResponseBody
    public boolean checkPayment(@PathVariable("orderId") Long orderId) {
        return alipayService.checkPayment(orderId);
    }

    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody //查询本地支付记录
    public PaymentInfo getPaymentInfo(@PathVariable("outTradeNo") String outTradeNo) {
        return paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
    }
}
