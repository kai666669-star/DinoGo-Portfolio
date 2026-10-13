package com.dinogo.sales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 建立訂單的輸入或目前商品狀態不符合業務規則。 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrderException extends IllegalArgumentException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
