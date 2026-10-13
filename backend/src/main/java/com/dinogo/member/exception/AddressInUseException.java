package com.dinogo.member.exception;

/** 地址已被訂單引用時，禁止直接刪除歷史關聯。 */
public class AddressInUseException extends RuntimeException {

    public AddressInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
