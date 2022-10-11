package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import jdk.nashorn.internal.scripts.JO;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    @Override
    public String createAliPay(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        if ("CLOSED".equals(orderInfo.getOrderStatus())) return "该订单已经取消！";
        else if ("PAID".equals(orderInfo.getOrderStatus())) return "该订单已支付！";
        // 生产二维码
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        // 参数
        // 声明一个map 集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //map.put("total_amount", orderInfo.getTotalAmount());
        map.put("total_amount", 0.01);//设为1分钱做测试
        map.put("subject", "test");
        map.put("timeout_express", "5m"); //设置五分钟过期
        alipayRequest.setBizContent(JSON.toJSONString(map));
        return alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单;
    }

    @SneakyThrows(AlipayApiException.class)
    @Override
    public boolean refund(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
        HashMap<String, Object> map = new HashMap<>();
        String outTradeNo = orderInfo.getOutTradeNo();
        map.put("out_trade_no", outTradeNo);
        map.put("refund_amount", "0.01");//退款金额小于支付金额
        map.put("refund_reason", "不好使！");
        refundRequest.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = alipayClient.execute(refundRequest);
        if (response.isSuccess()) {
            System.out.println(">>>>>>>>>>>>>>>>>>");
            System.out.println("退款成功！");
            System.out.println("<<<<<<<<<<<<<<<<<<");
            //电商平台订单关闭
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus("CLOSED");
            paymentService.updatePaymentInfo(outTradeNo, PaymentType.ALIPAY.name(), paymentInfo);
            return true;
        } else {
            System.out.println("!!!!!!!!!!!!!!!!!!");
            System.out.println("退款失败！");
            System.out.println("!!!!!!!!!!!!!!!!!!");
        }
        return false;
    }

    @SneakyThrows(AlipayApiException.class)
    @Override//关闭支付宝交易
    public boolean closePay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeCloseRequest closeRequest = new AlipayTradeCloseRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("operator_id", "YX01");
        closeRequest.setBizContent(JSON.toJSONString(map));
        AlipayTradeCloseResponse response = alipayClient.execute(closeRequest);
        if (response.isSuccess()) {
            System.out.println("关闭成功！");
            return true;
        }
        System.out.println("关闭失败！");
        return false;
    }

    @SneakyThrows(AlipayApiException.class)
    @Override
    public boolean checkPayment(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        queryRequest.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);
        if (response.isSuccess()) {
            System.out.println("查询记录成功！");
            return true;
        }
        System.out.println("查询记录失败！");
        return false;
    }


}
