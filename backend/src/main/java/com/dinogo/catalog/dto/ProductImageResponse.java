package com.dinogo.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private Integer imageId;

    private String imageUrl;

    private Integer sortOrder;

    private Boolean isMain;
}