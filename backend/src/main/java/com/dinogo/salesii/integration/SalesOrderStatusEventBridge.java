package com.dinogo.salesii.integration;

//rev+msg-start，總共1次修改，第1次//
import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.dinogo.sales.controller.EcpayCallbackController;
import com.dinogo.sales.service.OrderService;
import com.dinogo.sales.service.PaymentExpiryService;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.salesii.event.OrderStatusChangedEvent;

/**
 * 功能：在既有 Sales 寫入方法成功返回後，仍於原交易內發布補充事件。
 * 應用：事件只帶 orderId；Sysmsg 必須等原交易 commit 後再回查權威訂單，
 * 不修改 com.dinogo.sales 原始碼或信任事件內的狀態快照。
 */
@Component
public class SalesOrderStatusEventBridge implements BeanPostProcessor, Ordered {

    private static final Map<Class<?>, Set<String>> OBSERVED_METHODS = Map.of(
            OrderService.class, Set.of("updateStatusBySeller", "cancelOrder"),
            PaymentService.class, Set.of("simulatePaymentResult"),
            PaymentExpiryService.class, Set.of("expire"),
            ShipmentService.class, Set.of("updateShipmentStatus", "confirmDelivery", "simulateTcatEvent"),
            EcpayCallbackController.class, Set.of("callback"));

    private final ApplicationEventPublisher events;
    private final ObjectProvider<PaymentRepository> payments;

    public SalesOrderStatusEventBridge(
            ApplicationEventPublisher events,
            ObjectProvider<PaymentRepository> payments) {
        this.events = events;
        this.payments = payments;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Set<String> methods = OBSERVED_METHODS.get(targetClass);
        if (methods == null) {
            return bean;
        }

        MethodInterceptor advice = invocation -> {
            Object result = invocation.proceed();
            if (!methods.contains(invocation.getMethod().getName())) {
                return result;
            }
            Integer orderId = resolveOrderId(targetClass, invocation.getArguments());
            if (orderId != null) {
                events.publishEvent(new OrderStatusChangedEvent(orderId));
            }
            return result;
        };

        if (bean instanceof Advised advised) {
            advised.addAdvice(advice);
            return bean;
        }
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(advice);
        return proxyFactory.getProxy();
    }

    private Integer resolveOrderId(Class<?> targetClass, Object[] arguments) {
        if (arguments.length == 0) {
            return null;
        }
        if (arguments[0] instanceof Integer orderId) {
            return orderId;
        }
        if (targetClass == EcpayCallbackController.class && arguments[0] instanceof Map<?, ?> fields) {
            Object merchantTradeNo = fields.get("MerchantTradeNo");
            if (!(merchantTradeNo instanceof String paymentNo) || paymentNo.isBlank()) {
                return null;
            }
            return payments.getObject().findByPaymentNo(paymentNo)
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                    .map(payment -> payment.getOrder().getOrderId())
                    .orElse(null);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
//rev+msg-end，總共1次修改，第1次//
