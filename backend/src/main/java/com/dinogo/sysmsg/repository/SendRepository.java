package com.dinogo.sysmsg.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;

/** sysmsg.send 的一般查詢；流水號鎖定由 MsgFunctionSequenceRepository 負責。 */
public interface SendRepository extends JpaRepository<SendEntity, Integer> {

    @Query("""
            select s from SendEntity s
            where s.msgfromSellerId = :senderId and s.sendStatus = :status
              and (:prefix is null or s.msgFunction like concat(:prefix, '%'))
            """)
    Page<SendEntity> findBySenderAndStatus(
            @Param("senderId") Integer senderId, @Param("status") SendStatus status,
            @Param("prefix") String prefix,
            Pageable pageable);

    //sysmsg-start，總共1次修改，第1次//
    @Query("""
            select s from SendEntity s
            where s.sendStatus = :status
              and (s.msgFunction like 'OA%' or s.msgFunction like 'OC%' or s.msgFunction like 'OS%')
            """)
    Page<SendEntity> findSystemTemplates(
            @Param("status") SendStatus status,
            Pageable pageable);
    //sysmsg-end，總共1次修改，第1次//

    List<SendEntity> findByMsgFunctionAndSendStatus(
            String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgfromSellerIdAndMsgFunctionAndSendStatus(
            Integer msgfromSellerId, String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgFunctionOrderBySendUpdAtDesc(String msgFunction);

    List<SendEntity> findByMsgFunctionAndSendStatusOrderBySendUpdAtDesc(
            String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgfromSellerIdAndMsgFunctionOrderBySendUpdAtDesc(
            Integer msgfromSellerId, String msgFunction);

    List<SendEntity> findBySendStatusOrderBySendUpdAtDesc(SendStatus sendStatus);

    boolean existsByMsgFunctionAndSendStatus(
            String msgFunction, SendStatus sendStatus);
}
