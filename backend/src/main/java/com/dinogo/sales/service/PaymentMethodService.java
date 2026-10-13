package com.dinogo.sales.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.dto.payment.PaymentMethodResponse;
import com.dinogo.sales.repository.PaymentMethodRepository;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods() {
        return paymentMethodRepository.findAllByOrderByPaymentMethodIdAsc()
                .stream()
                .map(method -> new PaymentMethodResponse(
                        method.getMethodCode(),
                        method.getMethodName()))
                .toList();
    }
}
