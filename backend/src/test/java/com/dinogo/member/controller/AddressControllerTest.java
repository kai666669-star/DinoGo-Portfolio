package com.dinogo.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.member.dto.AddressRequest;
import com.dinogo.member.dto.AddressResponse;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.exception.AddressInUseException;
import com.dinogo.member.service.AddressService;
import com.dinogo.security.AuthenticatedMember;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    // Controller 依賴與登入會員測試資料。
    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private final AuthenticatedMember authenticatedMember =
            new AuthenticatedMember(1, "user@example.com");

    // 查詢 API 測試。
    @Test
    void getAddressesUsesAuthenticatedMemberId() {
        AddressResponse address = response(1, true);
        when(addressService.getAddresses(1)).thenReturn(List.of(address));

        ResponseEntity<?> result = addressController.getAddresses(authenticatedMember);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(List.of(address));
        verify(addressService).getAddresses(1);
    }

    @Test
    void getAddressReturnsStructuredNotFoundError() {
        when(addressService.getAddress(1, 9))
                .thenThrow(new IllegalArgumentException("Address not found"));

        ResponseEntity<?> result = addressController.getAddress(authenticatedMember, 9);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                HttpStatus.NOT_FOUND,
                "Address not found"));
    }

    // 新增與修改 API 測試。
    @Test
    void createAddressReturnsCreated() {
        AddressRequest request = request(false);
        AddressResponse response = response(1, true);
        when(addressService.createAddress(1, request)).thenReturn(response);

        ResponseEntity<?> result = addressController.createAddress(authenticatedMember, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(addressService).createAddress(1, request);
    }

    @Test
    void updateAddressUsesAuthenticatedMemberId() {
        AddressRequest request = request(true);
        AddressResponse response = response(2, true);
        when(addressService.updateAddress(1, 2, request)).thenReturn(response);

        ResponseEntity<?> result = addressController.updateAddress(authenticatedMember, 2, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(addressService).updateAddress(1, 2, request);
    }

    // 刪除 API 與 FK 衝突測試。
    @Test
    void deleteAddressReturnsNoContent() {
        ResponseEntity<?> result = addressController.deleteAddress(authenticatedMember, 2);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(addressService).deleteAddress(1, 2);
    }

    @Test
    void deleteAddressInUseReturnsConflict() {
        AddressInUseException exception = new AddressInUseException(
                "此地址已被訂單使用，無法刪除",
                new DataIntegrityViolationException("FK violation"));
        org.mockito.Mockito.doThrow(exception)
                .when(addressService).deleteAddress(1, 2);

        ResponseEntity<?> result = addressController.deleteAddress(authenticatedMember, 2);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody()).isEqualTo(
                MemberApiErrorResponse.from(
                        HttpStatus.CONFLICT,
                        "此地址已被訂單使用，無法刪除"));
    }

    // 測試 request 與 response 建立方法。
    private AddressRequest request(boolean isDefault) {
        return new AddressRequest(
                "王小明",
                "0912345678",
                "100",
                "台北市",
                "中正區",
                "忠孝西路一段 1 號",
                isDefault);
    }

    private AddressResponse response(Integer addressId, boolean isDefault) {
        return new AddressResponse(
                addressId,
                "王小明",
                "0912345678",
                "100",
                "台北市",
                "中正區",
                "忠孝西路一段 1 號",
                isDefault);
    }
}
