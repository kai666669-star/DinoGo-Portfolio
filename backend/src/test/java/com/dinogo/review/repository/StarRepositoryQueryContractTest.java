package com.dinogo.review.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

//review-start，總共1次修改，第1次//
/** 鎖定最終版 Offset 與單次聚合查詢契約。 */
class StarRepositoryQueryContractTest {

    @Test
    void publicReviewPageSupportsRatingAndContentFilters() throws Exception {
        Query query = StarRepository.class.getMethod(
                "findPublicProductReviews",
                Integer.class,
                Integer.class,
                String.class,
                Pageable.class).getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ").toLowerCase();
        assertNotNull(query);
        assertTrue(jpql.contains(":rating is null"));
        assertTrue(jpql.contains(":contentfilter = 'feedback'"));
        assertTrue(jpql.contains(":contentfilter = 'image'"));
    }

    @Test
    void productSummaryIsOneNativeAggregateQuery() throws Exception {
        Query query = StarRepository.class
                .getMethod("aggregateProductReviews", Integer.class)
                .getAnnotation(Query.class);
        String sql = query.value().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(query.nativeQuery());
        assertTrue(sql.contains("avg(cast(s.five_star"));
        assertTrue(sql.contains("count_big(*)"));
        assertTrue(sql.contains("withimagecount"));
    }

    @Test
    void sellerSummaryUsesOneIndexedJoinAggregate() throws Exception {
        Query query = StarRepository.class
                .getMethod("aggregateSellerRatings", Integer.class)
                .getAnnotation(Query.class);
        String sql = query.value().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("inner join review.history"));
        assertTrue(sql.contains("h.seller_id = :sellerid"));
        assertTrue(sql.contains("count_big(distinct s.product_id)"));
    }
}
//review-end，總共1次修改，第1次//
