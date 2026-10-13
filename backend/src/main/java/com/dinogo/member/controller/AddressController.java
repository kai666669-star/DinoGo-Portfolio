package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.member.dto.AddressRequest;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.exception.AddressInUseException;
import com.dinogo.member.service.AddressService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/** 收件地址 API；會員身分只使用 JWT principal，不接受前端傳入 memberId。 */
@Validated
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    // 地址業務邏輯統一交由 Service 處理。
    private final AddressService addressService;

    /** 使用 constructor injection 注入 AddressService。 */
    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /** 提供會員中心與結帳頁取得目前會員的地址清單。 */
    @GetMapping
    public ResponseEntity<?> getAddresses(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ResponseEntity.ok(addressService.getAddresses(member.memberId()));
    }

    /** 查詢目前會員擁有的單筆地址。 */
    @GetMapping("/{addressId}")
    public ResponseEntity<?> getAddress(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable @Positive(message = "Address ID 必須大於 0") Integer addressId) {
        try {
            return ResponseEntity.ok(addressService.getAddress(member.memberId(), addressId));
        } catch (IllegalArgumentException exception) {
            return notFound(exception);
        }
    }

    /** 新增目前會員的地址。 */
    @PostMapping
    public ResponseEntity<?> createAddress(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody AddressRequest request) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(addressService.createAddress(member.memberId(), request));
        } catch (IllegalArgumentException exception) {
            return notFound(exception);
        }
    }

    /** 修改目前會員擁有的地址。 */
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable @Positive(message = "Address ID 必須大於 0") Integer addressId,
            @Valid @RequestBody AddressRequest request) {
        try {
            return ResponseEntity.ok(
                    addressService.updateAddress(member.memberId(), addressId, request));
        } catch (IllegalArgumentException exception) {
            return notFound(exception);
        }
    }

    /** 刪除目前會員擁有且未被訂單引用的地址。 */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable @Positive(message = "Address ID 必須大於 0") Integer addressId) {
        try {
            addressService.deleteAddress(member.memberId(), addressId);
            return ResponseEntity.noContent().build();
        } catch (AddressInUseException exception) {
            return conflict(exception);
        } catch (IllegalArgumentException exception) {
            return notFound(exception);
        }
    }

    /** 將不存在或不屬於會員的地址轉成 404。 */
    private ResponseEntity<MemberApiErrorResponse> notFound(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(MemberApiErrorResponse.from(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    /** 將地址 FK 衝突轉成 409，避免回傳不明確的 500。 */
    private ResponseEntity<MemberApiErrorResponse> conflict(AddressInUseException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(MemberApiErrorResponse.from(HttpStatus.CONFLICT, exception.getMessage()));
    }
}
