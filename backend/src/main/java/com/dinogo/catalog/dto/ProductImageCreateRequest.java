package com.dinogo.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageCreateRequest {

    private String imageUrl;

    private Integer sortOrder;
}