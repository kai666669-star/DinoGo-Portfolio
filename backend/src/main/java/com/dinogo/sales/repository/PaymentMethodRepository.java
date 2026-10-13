package com.dinogo.sales.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sales.entity.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    List<PaymentMethod> findAllByOrderByPaymentMethodIdAsc();

    Optional<PaymentMethod> findByMethodCode(String methodCode);
}
