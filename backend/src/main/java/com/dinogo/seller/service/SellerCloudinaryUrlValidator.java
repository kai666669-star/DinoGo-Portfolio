package com.dinogo.seller.service;

import java.net.URI;
import java.net.URISyntaxException;

final class SellerCloudinaryUrlValidator {

    private static final String CLOUDINARY_HOST = "res.cloudinary.com";
    private static final String IMAGE_UPLOAD_PATH_MARKER = "/image/upload/";

    private SellerCloudinaryUrlValidator() {
    }

    static String optionalCloudinaryImageUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if (!isCloudinaryImageUrl(trimmed)) {
            throw new IllegalArgumentException(fieldName + " 必須是 Cloudinary HTTPS 圖片網址");
        }

        return trimmed;
    }

    private static boolean isCloudinaryImageUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            return "https".equalsIgnoreCase(scheme)
                    && CLOUDINARY_HOST.equalsIgnoreCase(host)
                    && path != null
                    && path.contains(IMAGE_UPLOAD_PATH_MARKER);
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
