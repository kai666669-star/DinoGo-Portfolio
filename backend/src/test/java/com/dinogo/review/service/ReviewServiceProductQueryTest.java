package com.dinogo.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.review.repository.ProductReviewAggregate;
import com.dinogo.review.repository.StarRepository;

class ReviewServiceProductQueryTest {

    private StarRepository stars;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        stars = mock(StarRepository.class);
        ProductReviewAggregate aggregate = mock(ProductReviewAggregate.class);
        when(stars.aggregateProductReviews(9)).thenReturn(aggregate);
        service = new ReviewService(
                mock(HistoryRepository.class),
                stars,
                mock(MonolithValidationService.class),
                mock(ReviewImageService.class));
    }

    @Test
    void forwardsFiltersAndUsesTenItemsPerPage() {
        when(stars.findPublicProductReviews(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(3), 0));

        ProductReviewPageResponse response = service.getProductReviews(9, 1, 5, "feedback");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(stars).findPublicProductReviews(
                org.mockito.ArgumentMatchers.eq(9),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("FEEDBACK"),
                pageable.capture());
        assertEquals(10, pageable.getValue().getPageSize());
        assertEquals(1, response.currentPage());
    }

    @Test
    void rejectsUnsupportedContentFilter() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getProductReviews(9, 1, null, "video"));
    }
}
