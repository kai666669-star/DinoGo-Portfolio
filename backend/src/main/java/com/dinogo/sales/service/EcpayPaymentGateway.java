package com.dinogo.sales.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dinogo.sales.dto.payment.EcpayCheckout;
import com.dinogo.sales.entity.Payment;

@Service
public class EcpayPaymentGateway {
    private final boolean enabled;
    private final String merchantId, hashKey, hashIv, checkoutUrl, returnUrl, orderResultUrl;
    public EcpayPaymentGateway(@Value("${app.ecpay.enabled:false}") boolean enabled,
            @Value("${app.ecpay.merchant-id:}") String merchantId, @Value("${app.ecpay.hash-key:}") String hashKey,
            @Value("${app.ecpay.hash-iv:}") String hashIv,
            @Value("${app.ecpay.checkout-url:https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5}") String checkoutUrl,
            @Value("${app.ecpay.return-url:}") String returnUrl, @Value("${app.ecpay.order-result-url:}") String orderResultUrl) {
        this.enabled = enabled; this.merchantId = merchantId; this.hashKey = hashKey; this.hashIv = hashIv;
        this.checkoutUrl = checkoutUrl; this.returnUrl = returnUrl; this.orderResultUrl = orderResultUrl;
    }
    public boolean isEnabled() { return enabled; }
    public String merchantId() { return merchantId; }
    public EcpayCheckout checkout(Payment payment) {
        if (!enabled || merchantId.isBlank() || hashKey.isBlank() || hashIv.isBlank() || returnUrl.isBlank()) throw new IllegalStateException("ECPay is not configured");
        Map<String,String> p = new LinkedHashMap<>();
        p.put("MerchantID", merchantId); p.put("MerchantTradeNo", payment.getPaymentNo());
        p.put("MerchantTradeDate", ZonedDateTime.now(ZoneId.of("Asia/Taipei")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        p.put("PaymentType", "aio"); p.put("TotalAmount", payment.getAmount().toBigIntegerExact().toString());
        p.put("TradeDesc", "DinoGo order"); p.put("ItemName", "DinoGo order " + payment.getOrder().getOrderNo());
        p.put("ReturnURL", returnUrl); if (!orderResultUrl.isBlank()) p.put("OrderResultURL", orderResultUrl);
        p.put("ChoosePayment", "Credit"); p.put("EncryptType", "1"); p.put("CheckMacValue", cmv(p));
        return new EcpayCheckout(checkoutUrl, Map.copyOf(p));
    }
    public boolean verify(Map<String,String> p) { return enabled && merchantId.equals(p.get("MerchantID")) && MessageDigest.isEqual(cmv(p).getBytes(StandardCharsets.UTF_8), p.getOrDefault("CheckMacValue", "").getBytes(StandardCharsets.UTF_8)); }
    private String cmv(Map<String,String> p) {
        TreeMap<String,String> s = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); p.forEach((k,v)-> { if (!"CheckMacValue".equalsIgnoreCase(k)) s.put(k,v); });
        String joined = s.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).reduce((a,b)->a+"&"+b).orElse("");
        String raw = "HashKey="+hashKey+"&"+joined+"&HashIV="+hashIv;
        try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(URLEncoder.encode(raw, StandardCharsets.UTF_8).toLowerCase().replace("%2d","-").replace("%5f","_").replace("%2e",".").replace("%21","!").replace("%2a","*").replace("%28","(").replace("%29",")").getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(hash).toUpperCase(); } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
