package com.dinogo.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.repository.PaymentMethodRepository;

class PaymentMethodServiceTest {

    @Test
    void getPaymentMethodsReturnsDatabaseCodesAndNames() {
        PaymentMethodRepository repository = Mockito.mock(PaymentMethodRepository.class);
        PaymentMethod creditCard = paymentMethod("CREDIT_CARD", "信用卡");
        PaymentMethod cashOnDelivery = paymentMethod("CASH_ON_DELIVERY", "貨到付款");
        when(repository.findAllByOrderByPaymentMethodIdAsc())
                .thenReturn(List.of(creditCard, cashOnDelivery));

        var response = new PaymentMethodService(repository).getPaymentMethods();

        assertThat(response)
                .extracting(method -> method.paymentMethodCode() + ":" + method.paymentMethodName())
                .containsExactly("CREDIT_CARD:信用卡", "CASH_ON_DELIVERY:貨到付款");
    }

    private PaymentMethod paymentMethod(String code, String name) {
        PaymentMethod method = new PaymentMethod();
        method.setMethodCode(code);
        method.setMethodName(name);
        return method;
    }
}
