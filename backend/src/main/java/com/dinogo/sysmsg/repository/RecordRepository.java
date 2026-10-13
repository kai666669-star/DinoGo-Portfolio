package com.dinogo.sysmsg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.RecordStatus;
import com.dinogo.sysmsg.entity.MemberInbox;
import com.dinogo.sysmsg.entity.SellerInbox;

public interface RecordRepository
        extends JpaRepository<RecordEntity, Integer> {

    @EntityGraph(attributePaths = "send")
    Page<RecordEntity> findAllByOrderByRecordCreatedAtDescRecordIdDesc(Pageable pageable);

    /** Email dispatcher 需在同一次查詢取得 Record 與訊息母件。 */
    @EntityGraph(attributePaths = "send")
    Optional<RecordEntity> findWithSendByRecordId(Integer recordId);

    // ============================================================
    // 會員收件匣
    // ============================================================

    List<RecordEntity>
    findByMsgtoMemberIdAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoMemberId,
            RecordStatus recordStatus
    );

    @EntityGraph(attributePaths = "send")
    @Query("""
            select r from RecordEntity r
            where r.msgtoMemberId = :recipientId and r.memberInbox = :inbox
              and r.recordStatus <> :excludedStatus
            """)
    Page<RecordEntity> findMemberInbox(
            @Param("recipientId") Integer recipientId, @Param("inbox") MemberInbox inbox,
            @Param("excludedStatus") RecordStatus excludedStatus,
            Pageable pageable);

    //msg-首頁通知未讀// 首頁通知紅色圓點使用的會員未讀訊息總數。
    long countByMsgtoMemberIdAndRecordStatus(Integer msgtoMemberId, RecordStatus recordStatus);

    Page<RecordEntity>
    findByMsgtoMemberIdAndRecordStatusOrderByRecordCreatedAtDescRecordIdDesc(
            Integer msgtoMemberId,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 會員收件匣 + msg_function
    // ============================================================

    List<RecordEntity>
    findByMsgtoMemberIdAndMsgFunctionAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoMemberId,
            String msgFunction,
            RecordStatus recordStatus
    );

    Page<RecordEntity>
    findByMsgtoMemberIdAndMsgFunctionStartingWithAndRecordStatusOrderByRecordCreatedAtDescRecordIdDesc(
            Integer msgtoMemberId,
            String msgFunctionPrefix,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 商家收件匣
    // ============================================================

    List<RecordEntity>
    findByMsgtoSellerIdAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoSellerId,
            RecordStatus recordStatus
    );

    @EntityGraph(attributePaths = "send")
    @Query("""
            select r from RecordEntity r
            where r.msgtoSellerId = :recipientId and r.sellerInbox = :inbox
              and r.recordStatus <> :excludedStatus
              and (r.sellerInbox <> :newOrderInbox or r.orderStatus in :newOrderStatuses)
            """)
    Page<RecordEntity> findSellerInbox(
            @Param("recipientId") Integer recipientId, @Param("inbox") SellerInbox inbox,
            @Param("excludedStatus") RecordStatus excludedStatus,
            @Param("newOrderInbox") SellerInbox newOrderInbox,
            @Param("newOrderStatuses") List<String> newOrderStatuses,
            Pageable pageable);

    @Query("""
            select count(r) from RecordEntity r
            where r.msgtoSellerId = :recipientId and r.sellerInbox = :inbox
              and r.recordStatus = :status
              and (r.sellerInbox <> :newOrderInbox or r.orderStatus in :newOrderStatuses)
            """)
    long countSellerInbox(
            @Param("recipientId") Integer recipientId, @Param("inbox") SellerInbox inbox,
            @Param("status") RecordStatus status,
            @Param("newOrderInbox") SellerInbox newOrderInbox,
            @Param("newOrderStatuses") List<String> newOrderStatuses);

    Page<RecordEntity>
    findByMsgtoSellerIdAndRecordStatusOrderByRecordCreatedAtDescRecordIdDesc(
            Integer msgtoSellerId,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 商家收件匣 + msg_function
    // ============================================================

    List<RecordEntity>
    findByMsgtoSellerIdAndMsgFunctionAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoSellerId,
            String msgFunction,
            RecordStatus recordStatus
    );

    Page<RecordEntity>
    findByMsgtoSellerIdAndMsgFunctionStartingWithAndRecordStatusOrderByRecordCreatedAtDescRecordIdDesc(
            Integer msgtoSellerId,
            String msgFunctionPrefix,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 權限驗證
    //
    // 會員是否擁有這筆 Record
    // ============================================================

    Optional<RecordEntity>
    findByRecordIdAndMsgtoMemberId(
            Integer recordId,
            Integer msgtoMemberId
    );


    // ============================================================
    // 權限驗證
    //
    // 商家是否擁有這筆 Record
    // ============================================================

    Optional<RecordEntity>
    findByRecordIdAndMsgtoSellerId(
            Integer recordId,
            Integer msgtoSellerId
    );


    // ============================================================
    // 發送紀錄
    //
    // 某個 SEND 建立過哪些 Record
    // ============================================================

    List<RecordEntity>
    findBySend_SendIdOrderByRecordCreatedAtDesc(
            Integer sendId
    );


    // ============================================================
    // 發送紀錄
    //
    // 某個 msg_function 的發送紀錄
    // ============================================================

    List<RecordEntity>
    findByMsgFunctionOrderByRecordCreatedAtDesc(
            String msgFunction
    );


    // ============================================================
    // 發送者紀錄
    // ============================================================

    List<RecordEntity>
    findByMsgfromSellerIdOrderByRecordCreatedAtDesc(
            Integer msgfromSellerId
    );

    boolean existsBySend_SendId(Integer sendId);

    boolean existsByOrderIdAndOrderStatusAndMsgtoMemberId(
            Integer orderId,
            String orderStatus,
            Integer msgtoMemberId
    );

    boolean existsByOrderIdAndOrderStatusAndMsgtoSellerId(
            Integer orderId,
            String orderStatus,
            Integer msgtoSellerId
    );
}
