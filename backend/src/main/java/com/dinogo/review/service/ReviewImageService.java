package com.dinogo.review.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.review.dto.response.ReviewImageAssetResponse;
import com.dinogo.review.exception.ReviewConflictException;

//review-start，總共1次修改，第1次//
/** Review 自有的 Cloudinary 上傳、檔案辨識、資產歸屬驗證與失敗補償服務。 */
@Service
public class ReviewImageService {
    private static final int MAX_FILES = 3;
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final String HOST = "res.cloudinary.com";
    private static final Map<String, String> MIME_BY_FORMAT = Map.of(
            "jpg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp", "image/webp");

    private final Cloudinary cloudinary;
    private final String cloudName;

    public ReviewImageService(Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name}") String cloudName) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
    }

    public List<ReviewImageAssetResponse> upload(List<MultipartFile> files, Integer memberId) {
        requirePositiveMemberId(memberId);
        List<ValidatedImage> validated = validateAll(files);
        String ownerPrefix = "dinogo/reviews/" + memberId;
        List<String> uploadedPublicIds = new ArrayList<>();
        List<ReviewImageAssetResponse> results = new ArrayList<>();
        try {
            for (ValidatedImage image : validated) {
                String requestedPublicId = ownerPrefix + "/" + UUID.randomUUID();
                Map<?, ?> result = cloudinary.uploader().upload(image.bytes(), ObjectUtils.asMap(
                        "public_id", requestedPublicId, "resource_type", "image",
                        "overwrite", false, "unique_filename", false));
                uploadedPublicIds.add(requestedPublicId);
                results.add(toResponse(result, requestedPublicId, ownerPrefix));
            }
            return List.copyOf(results);
        } catch (Exception exception) {
            compensate(uploadedPublicIds);
            throw new ReviewConflictException("Cloudinary 評論圖片上傳失敗", exception);
        }
    }

    public void validateReference(String url, String publicId, String ownerPrefix) {
        if (isBlank(url) && isBlank(publicId)) return;
        if (isBlank(url) || isBlank(publicId)) {
            throw new IllegalArgumentException("評論圖片 URL 與 publicId 必須同時提供");
        }
        requireOwnedPublicId(publicId, ownerPrefix);
        try {
            URI uri = URI.create(url);
            String expectedPrefix = "/" + cloudName + "/image/upload/";
            String path = uri.getPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !HOST.equalsIgnoreCase(uri.getHost())
                    || path == null || !path.startsWith(expectedPrefix)
                    || !path.matches("^" + java.util.regex.Pattern.quote(expectedPrefix)
                            + "(?:v\\d+/)?" + java.util.regex.Pattern.quote(publicId)
                            + "\\.(?:jpg|jpeg|png|gif|webp)$")) {
                throw new IllegalArgumentException("圖片不是目前 DinoGo Review Cloudinary 資產");
            }
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("圖片不是")) throw exception;
            throw new IllegalArgumentException("評論圖片 URL 格式不合法", exception);
        }
    }

    private List<ValidatedImage> validateAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw new IllegalArgumentException("評論圖片必須為 1～3 張");
        }
        List<ValidatedImage> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
                throw new IllegalArgumentException("評論圖片不可為空且每張不得超過 10MB");
            }
            try {
                byte[] bytes = file.getBytes();
                String format = detectFormat(bytes);
                String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
                if (!MIME_BY_FORMAT.get(format).equals(declared)) {
                    throw new IllegalArgumentException("評論圖片 Content-Type 與實際格式不符");
                }
                result.add(new ValidatedImage(bytes));
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalArgumentException("無法讀取評論圖片", exception);
            }
        }
        return result;
    }

    private String detectFormat(byte[] b) {
        if (b.length >= 3 && unsigned(b[0]) == 0xff && unsigned(b[1]) == 0xd8 && unsigned(b[2]) == 0xff) return "jpg";
        if (b.length >= 8 && unsigned(b[0]) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && unsigned(b[4]) == 0x0d && unsigned(b[5]) == 0x0a && unsigned(b[6]) == 0x1a && unsigned(b[7]) == 0x0a) return "png";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') return "gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new IllegalArgumentException("評論圖片只接受 JPEG、PNG、GIF 或 WebP");
    }

    private ReviewImageAssetResponse toResponse(Map<?, ?> result, String requestedId, String ownerPrefix) {
        String publicId = text(result.get("public_id"));
        String secureUrl = text(result.get("secure_url"));
        String assetId = text(result.get("asset_id"));
        String resourceType = text(result.get("resource_type"));
        requireOwnedPublicId(publicId, ownerPrefix);
        if (!requestedId.equals(publicId) || isBlank(assetId) || !"image".equals(resourceType)) {
            throw new IllegalStateException("Cloudinary 回傳的評論圖片資產資料不完整");
        }
        validateReference(secureUrl, publicId, ownerPrefix);
        return new ReviewImageAssetResponse(assetId, publicId, secureUrl, resourceType,
                text(result.get("format")), number(result.get("bytes")),
                integer(result.get("width")), integer(result.get("height")));
    }

    private void compensate(List<String> publicIds) {
        List<String> reverse = new ArrayList<>(publicIds);
        Collections.reverse(reverse);
        for (String publicId : reverse) {
            try {
                cloudinary.uploader().destroy(publicId,
                        ObjectUtils.asMap("resource_type", "image", "invalidate", true));
            } catch (Exception ignored) {
                // 補償刪除採 best effort；原始上傳錯誤仍優先回傳。
            }
        }
    }

    private void requireOwnedPublicId(String publicId, String ownerPrefix) {
        if (isBlank(publicId) || isBlank(ownerPrefix) || !publicId.startsWith(ownerPrefix + "/")
                || publicId.contains("..") || publicId.contains("\\")) {
            throw new IllegalArgumentException("評論圖片 publicId 不屬於目前會員");
        }
    }

    private void requirePositiveMemberId(Integer memberId) {
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("memberId 必須是正整數");
    }

    private int unsigned(byte value) { return value & 0xff; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String text(Object value) { return value == null ? null : value.toString(); }
    private long number(Object value) { return value instanceof Number n ? n.longValue() : 0L; }
    private Integer integer(Object value) { return value instanceof Number n ? n.intValue() : null; }
    private record ValidatedImage(byte[] bytes) {}
}
//review-end，總共1次修改，第1次//

