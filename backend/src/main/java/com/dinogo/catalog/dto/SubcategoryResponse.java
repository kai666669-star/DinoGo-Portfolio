package com.dinogo.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubcategoryResponse {

    private Integer subcategoryId;
    private Integer categoryId;
    private String subcategoryName;
}