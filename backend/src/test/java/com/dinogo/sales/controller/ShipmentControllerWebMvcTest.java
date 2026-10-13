package com.dinogo.sales.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.dto.shipment.ShipmentResponse;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(ShipmentController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class ShipmentControllerWebMvcTest {

    private static final String ENDPOINT = "/api/orders/10/shipment/tracking-info";
    private static final String VALID_REQUEST = """
            {"carrierName":"Black Cat","trackingNo":"TRACK-1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void sellerCanUpdateTrackingInfo() throws Exception {
        when(shipmentService.updateShipmentTrackingInfo(eq(10), eq(8), any()))
                .thenReturn(response());

        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrierName").value("Black Cat"))
                .andExpect(jsonPath("$.trackingNo").value("TRACK-1"));

        verify(shipmentService).updateShipmentTrackingInfo(eq(10), eq(8), any());
    }

    @Test
    void blankTrackingInfoReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"   \",\"trackingNo\":\"\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void blankShipmentCreationReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders/10/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"   \",\"trackingNo\":\"\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void trackingInfoLongerThanDatabaseColumnsReturnsBadRequest() throws Exception {
        String tooLong = "X".repeat(101);
        String request = "{\"carrierName\":\"" + tooLong
                + "\",\"trackingNo\":\"" + tooLong + "\"}";

        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void unauthenticatedMemberCannotUpdateTrackingInfo() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void buyerCannotUpdateTrackingInfo() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void missingShipmentReturnsNotFound() throws Exception {
        when(shipmentService.updateShipmentTrackingInfo(eq(10), eq(8), any()))
                .thenThrow(new OrderNotFoundException("Shipment does not exist"));

        mockMvc.perform(patch(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST)
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shipment does not exist"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    private UsernamePasswordAuthenticationToken authenticationForRole(String role) {
        AuthenticatedMember principal = new AuthenticatedMember(8, "member@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private ShipmentResponse response() {
        return new ShipmentResponse(
                20,
                10,
                "Black Cat",
                "TRACK-1",
                ShipmentStatus.PREPARING,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
