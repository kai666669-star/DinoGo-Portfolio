package com.dinogo.review.dto.response;

import java.util.List;

import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;

/** 會員訂單頁使用的評論主檔與商品細項 Response，不直接將 Entity 傳給前端。 */
public record HistoryResponse(
        Integer historyId,
        Integer sellerId,
        Integer memberId,
        Integer orderId,
        //review-start，總共1次修改，第1次//
        String orderNo,
        //review-end，總共1次修改，第1次//
        List<StarResponse> stars) {

    /** 將 History 與已查出的 Star 清單轉成 API DTO。 */
    public static HistoryResponse fromEntity(
            HistoryEntity history,
            List<StarEntity> stars) {

        return new HistoryResponse(
                history.getId(),
                history.getSellerId(),
                history.getMemberId(),
                history.getOrderId(),
                history.getOrderNo(),
                stars.stream().map(StarResponse::fromEntity).toList());
    }
}
