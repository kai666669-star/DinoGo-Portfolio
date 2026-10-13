package com.dinogo.review.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;

class ProductReviewResponseTest {

    @Test
    void exposesOnlyMaskedMemberId() {
        HistoryEntity history = new HistoryEntity();
        history.setMemberId(12345);
        StarEntity star = new StarEntity();
        star.setHistory(history);

        ProductReviewResponse response = ProductReviewResponse.fromEntity(star);

        assertEquals("會員 1*****5", response.reviewerDisplayName());
    }
}
