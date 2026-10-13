package com.dinogo.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SellerCloudinaryUrlValidatorTest {

    @Test
    void acceptsCloudinaryHttpsImageUploadUrl() {
        String url = "https://res.cloudinary.com/demo/image/upload/sample.jpg";

        assertThat(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(url, "店鋪 Logo URL"))
                .isEqualTo(url);
    }

    @Test
    void rejectsNonCloudinaryImageUrl() {
        assertThatThrownBy(() -> SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(
                "https://example.com/logo.png",
                "店鋪 Logo URL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店鋪 Logo URL 必須是 Cloudinary HTTPS 圖片網址");
    }

    @Test
    void normalizesBlankOptionalUrl() {
        assertThat(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(null, "店鋪 Logo URL"))
                .isNull();
        assertThat(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl("", "店鋪 Logo URL"))
                .isNull();
    }
}
