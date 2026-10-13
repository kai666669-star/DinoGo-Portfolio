package com.dinogo.sales.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.dinogo.exception.OrderExceptionHandler;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;

class SellerOrderControllerTest {

    @Test
    void getSellerOrdersUsesAuthenticatedMember() {
        OrderService orderService = mock(OrderService.class);
        SellerOrderController controller = new SellerOrderController(orderService);
        AuthenticatedMember member = new AuthenticatedMember(6, "seller@example.com");
        when(orderService.getSellerOrders(6)).thenReturn(List.of());

        var result = controller.getSellerOrders(member, null, null);

        org.assertj.core.api.Assertions.assertThat(result.getBody()).isEmpty();
        verify(orderService).getSellerOrders(6, null, null);
    }

    @Test
    void getSellerOrderUsesAuthenticatedMember() {
        OrderService orderService = mock(OrderService.class);
        SellerOrderController controller = new SellerOrderController(orderService);
        AuthenticatedMember member = new AuthenticatedMember(6, "seller@example.com");
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(orderService.getSellerOrder(99, 6)).thenReturn(response);

        var result = controller.getSellerOrder(99, member);

        org.assertj.core.api.Assertions.assertThat(result.getBody()).isSameAs(response);
        verify(orderService).getSellerOrder(99, 6);
    }

    @Test
    void missingSellerOrderReturnsStructuredNotFoundResponse() throws Exception {
        OrderService orderService = mock(OrderService.class);
        when(orderService.getSellerOrder(99, 6))
                .thenThrow(new OrderNotFoundException("Order does not exist"));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SellerOrderController(orderService))
                .setControllerAdvice(new OrderExceptionHandler())
                .setCustomArgumentResolvers(authenticatedMemberResolver(6))
                .build();

        mockMvc.perform(get("/api/seller/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order does not exist"))
                .andExpect(jsonPath("$.path").value("/api/seller/orders/99"));
    }

    private HandlerMethodArgumentResolver authenticatedMemberResolver(Integer memberId) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return new AuthenticatedMember(memberId, "seller@example.com");
            }
        };
    }
}
