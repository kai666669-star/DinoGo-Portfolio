package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dinogo.sales.dto.payment.PaymentMethodResponse;
import com.dinogo.sales.service.PaymentMethodService;

class PaymentCapabilityControllerTest {

    @Test
    void getPaymentMethodsReturnsSharedPaymentMethods() {
        PaymentMethodService paymentMethodService = mock(PaymentMethodService.class);
        List<PaymentMethodResponse> methods = List.of(
                new PaymentMethodResponse("CASH_ON_DELIVERY", "貨到付款"));
        when(paymentMethodService.getPaymentMethods()).thenReturn(methods);

        var response = new PaymentCapabilityController(false, paymentMethodService)
                .getPaymentMethods();

        assertThat(response).isSameAs(methods);
    }
}
