package com.dinogo.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record FavoriteResponse(

                Integer favoriteId,

                Integer productId,

                String productName,

                BigDecimal basePrice,

                String imageUrl,

                Byte productStatus,

                List<FavoriteSkuResponse> skus,

                Boolean available) {
}