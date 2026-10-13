package com.dinogo.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageSortUpdateRequest {

    private Integer imageId;
    private Integer sortOrder;
}