package com.dinogo.review.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.review.entity.StarEntity;

/** Review Star 資料存取層；只描述查詢，不承擔權限與商業規則。 */

public interface StarRepository extends JpaRepository<StarEntity, Integer> {

  /** 功能：取得訂單的商品評論快照；應用：會員訂單頁依原品項順序顯示。 */
  List<StarEntity> findByHistoryIdOrderByOrderItemIdAsc(Integer historyId);

  // review-start，總共3次修改，第1次//
  /**
   * 功能：由 Spring Data 從同一方法衍生內容與 count 查詢。
   * 應用：Pageable 產生 SQL Server OFFSET/FETCH，避免列表和總筆數條件分歧。
   */
  @EntityGraph(attributePaths = "history")
  @Query("""
      SELECT s
      FROM StarEntity s
      WHERE s.productId = :productId
        AND s.fiveStar IS NOT NULL
        AND (:rating IS NULL OR s.fiveStar = :rating)
        AND (
              :contentFilter = 'ALL'
              OR (:contentFilter = 'FEEDBACK'
                  AND s.feedback IS NOT NULL
                  AND TRIM(s.feedback) <> '')
              OR (:contentFilter = 'IMAGE'
                  AND (s.imgOne IS NOT NULL OR s.imgTwo IS NOT NULL OR s.imgThree IS NOT NULL))
            )
      """)
  Page<StarEntity> findPublicProductReviews(
      @Param("productId") Integer productId,
      @Param("rating") Integer rating,
      @Param("contentFilter") String contentFilter,
      Pageable pageable);
  // review-end，總共3次修改，第1次//

  // review-start，總共3次修改，第2次//
  /** 功能：一次聚合產品評價完整摘要；應用：產品頁不再為九個數值分別查詢。 */
  @Query(value = """
      SELECT
          AVG(CAST(s.five_star AS decimal(3,1))) AS averageFiveStar,
          COUNT_BIG(*) AS totalCount,
          COALESCE(SUM(CASE WHEN s.five_star = 5 THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS fiveStarCount,
          COALESCE(SUM(CASE WHEN s.five_star = 4 THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS fourStarCount,
          COALESCE(SUM(CASE WHEN s.five_star = 3 THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS threeStarCount,
          COALESCE(SUM(CASE WHEN s.five_star = 2 THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS twoStarCount,
          COALESCE(SUM(CASE WHEN s.five_star = 1 THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS oneStarCount,
          COALESCE(SUM(CASE WHEN NULLIF(LTRIM(RTRIM(s.feedback)), '') IS NOT NULL
                   THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS withFeedbackCount,
          COALESCE(SUM(CASE WHEN NULLIF(LTRIM(RTRIM(s.img_one)), '') IS NOT NULL
                        OR NULLIF(LTRIM(RTRIM(s.img_two)), '') IS NOT NULL
                        OR NULLIF(LTRIM(RTRIM(s.img_three)), '') IS NOT NULL
                   THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END), 0) AS withImageCount
      FROM review.star s
      WHERE s.product_id = :productId
        AND s.five_star IS NOT NULL
      """, nativeQuery = true)
  ProductReviewAggregate aggregateProductReviews(@Param("productId") Integer productId);
  // review-end，總共3次修改，第2次//

  // review-start，總共3次修改，第3次//
  /** 功能：一次聚合符合公開條件的商家評價；應用：商家頁避免重複執行相同 JOIN。 */
  @Query(value = """
      SELECT
          AVG(CAST(s.five_star AS decimal(3,1))) AS averageFiveStar,
          COUNT_BIG(*) AS ratingCount,
          COUNT_BIG(DISTINCT s.product_id) AS ratedProductCount
      FROM review.star s
      INNER JOIN review.history h ON h.history_id = s.history_id
      INNER JOIN catalog.Product p ON p.product_id = s.product_id
      WHERE h.seller_id = :sellerId
        AND p.seller_id = :sellerId
        AND p.status = 1
        AND p.sold_count > 0
        AND s.five_star IS NOT NULL
      """, nativeQuery = true)
  SellerRatingAggregate aggregateSellerRatings(@Param("sellerId") Integer sellerId);
  // review-end，總共3次修改，第3次//

}
