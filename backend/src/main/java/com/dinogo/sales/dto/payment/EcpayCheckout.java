package com.dinogo.sales.dto.payment;

import java.util.Map;

public record EcpayCheckout(String action, Map<String, String> fields) {
}
