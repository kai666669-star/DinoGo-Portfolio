package com.dinogo.member.dto;

import com.dinogo.member.entity.Address;

/** 對前端公開的地址資料，不回傳 Member Entity。 */
public record AddressResponse(
        Integer addressId,
        String receiverName,
        String receiverPhone,
        String postalCode,
        String city,
        String district,
        String detailAddress,
        boolean isDefault
) {

    /** 將 Address Entity 轉成穩定的 API response。 */
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getPostalCode(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                Boolean.TRUE.equals(address.getIsDefault())
        );
    }
}
