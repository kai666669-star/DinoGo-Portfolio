package com.dinogo.sysmsg.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.dto.external.*;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;
import com.dinogo.sysmsg.service.mapper.RecordResponseMapper;
import static com.dinogo.sysmsg.service.validation.SysmsgValidation.positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class RecordServiceImpl implements RecordService {
    private final RecordRepository records;
    private final SendRepository sends;
    private final MsgPermissionService permissions;
    private final RecordChannelService channels;
    private final MemberSysmsgProviderService members; //Client-sysmsg
    private final SellerSysmsgProviderService sellers; //Client-sysmsg
    private final RecordResponseMapper responseMapper;

    public RecordServiceImpl(RecordRepository records, SendRepository sends, MsgPermissionService permissions,
            RecordChannelService channels, MemberSysmsgProviderService members, //Client-sysmsg
            SellerSysmsgProviderService sellers, RecordResponseMapper responseMapper) { //Client-sysmsg
        this.records = records;
        this.sends = sends;
        this.permissions = permissions;
        this.channels = channels;
        this.members = members;
        this.sellers = sellers;
        this.responseMapper = responseMapper;
    }

    @Override @Transactional
    public List<RecordResponse> createRecords(Integer sendId, List<Integer> memberIds, List<Integer> sellerIds) {
        SendEntity send = requireSend(sendId);
        if (send.getSendStatus() != SendStatus.SEND) throw new IllegalStateException("Record 只能指向 send_status=SEND：" + sendId);
        List<RecordResponse> result = new ArrayList<>();
        for (Integer id : distinct(memberIds)) {
            requireRecipientType(send, false);
            MemberAuthResponse member=ModuleDataMapper.member(members.getMember(positive(id, "member_id"))); //Client-sysmsg
            RecordEntity record = new RecordEntity(send, positive(id, "member_id"), null);
            record=records.save(record); channels.createEmailChannel(sendId,record.getRecordId(),member,send.getMsgFunction(),false);
            result.add(responseMapper.toResponse(record));
        }
        for (Integer id : distinct(sellerIds)) {
            requireRecipientType(send, true);
            SellerInfoResponse seller=ModuleDataMapper.seller(sellers.getSeller(positive(id, "seller_id"))); //Client-sysmsg
            if(!seller.isActive())throw new IllegalStateException("收件商家未啟用："+id);
            RecordEntity record = new RecordEntity(send, null, positive(id, "seller_id"));
            record=records.save(record); channels.createEmailChannel(sendId,record.getRecordId(),asMemberProfile(seller),send.getMsgFunction(),true);
            result.add(responseMapper.toResponse(record));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("至少需要一位收件人");
        return result;
    }

    /** 只供訂單通知建立帶快照的 Record；memberId、sellerId 必須二選一。 */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RecordResponse createOrderRecord(Integer sendId, Integer memberId, Integer sellerId, Integer orderId, String status) {
        SendEntity send = requireSend(sendId);
        if (send.getSendStatus() != SendStatus.SEND) throw new IllegalStateException("Send 尚未發送");
        RecordEntity record = new RecordEntity(send, memberId, sellerId);
        record.setOrderId(positive(orderId, "order_id")); record.setOrderStatus(status);
        record=records.save(record);
        // createEmailChannel 以 REQUIRED 加入同一交易；若應建立的 Channel 寫入失敗，
        // 例外必須向外傳遞，使 Record 與前面建立的 Send 一併 rollback。
        if(memberId!=null)channels.createEmailChannel(sendId,record.getRecordId(),ModuleDataMapper.member(members.getMember(memberId)),send.getMsgFunction(),false); //Client-sysmsg
        else channels.createEmailChannel(sendId,record.getRecordId(),asMemberProfile(ModuleDataMapper.seller(sellers.getSeller(sellerId))),send.getMsgFunction(),true); //Client-sysmsg
        return responseMapper.toResponse(record);
    }

    @Override @Transactional public RecordResponse createSingleMemberRecord(Integer sendId, Integer memberId) {
        return createRecords(sendId, List.of(memberId), List.of()).get(0);
    }
    @Override @Transactional public RecordResponse createSingleSellerRecord(Integer sendId, Integer sellerId) {
        return createRecords(sendId, List.of(), List.of(sellerId)).get(0);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean existsBySendId(Integer sendId) {
        return records.existsBySend_SendId(sendId);
    }
    @Override @Transactional(readOnly=true) public RecordResponse searchRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id);
        permissions.validateRecordOwner(r, loginId);
        return toDetailResponse(r);
    }
    @Override @Transactional public RecordResponse readRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id); permissions.validateRecordOwner(r, loginId);
        if (r.getRecordStatus() == RecordStatus.DELETE) throw new IllegalStateException("DELETE 訊息不能閱讀");
        if (r.getRecordStatus() == RecordStatus.UNREAD) r.setRecordStatus(RecordStatus.READ);
        return toDetailResponse(records.save(r));
    }
    @Override @Transactional public void deleteRecord(Integer id, Integer loginId) {
        RecordEntity r = requireRecord(id); permissions.validateRecordOwner(r, loginId);
        if (r.getRecordStatus() == RecordStatus.DELETE) throw new IllegalStateException("訊息已刪除");
        String p = prefix(r);
        if (Set.of("OA", "OC", "OS").contains(p)) {
            records.delete(r);
        } else {
            r.setRecordStatus(RecordStatus.DELETE);
            records.save(r);
        }
    }
    @Override @Transactional(readOnly=true)
    public OffsetPageResponse<MsgInboxResponse> getMemberInbox(
            Integer id, String inbox, Integer page) {
        MemberInbox wanted = MemberInbox.valueOf(inbox.trim().toUpperCase());
        return OffsetPageResponse.from(records.findMemberInbox(
                id, wanted, RecordStatus.DELETE, pageRequest(page))
                .map(responseMapper::toInboxResponse));
    }
    //msg-首頁通知未讀// 直接由 Repository COUNT，不把所有訊息載入記憶體。
    @Override @Transactional(readOnly=true) public long countMemberUnread(Integer memberId) {
        return records.countByMsgtoMemberIdAndRecordStatus(positive(memberId, "member_id"), RecordStatus.UNREAD);
    }
    @Override @Transactional(readOnly=true)
    public OffsetPageResponse<MsgInboxResponse> getSellerInbox(
            Integer id, String inbox, Integer page) {
        SellerInbox wanted = SellerInbox.valueOf(inbox.trim().toUpperCase());
        return OffsetPageResponse.from(records.findSellerInbox(
                id, wanted, RecordStatus.DELETE, SellerInbox.NEW_ORDER,
                List.of("PAID", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED"),
                pageRequest(page))
                .map(responseMapper::toInboxResponse));
    }

    @Override @Transactional(readOnly=true)
    public SellerUnreadCountsResponse countSellerUnread(Integer sellerId) {
        Integer recipientId = positive(sellerId, "seller_id");
        List<String> newOrderStatuses = List.of("PAID", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED");
        long systemNotice = records.countSellerInbox(recipientId, SellerInbox.SYSTEM_NOTICE,
                RecordStatus.UNREAD, SellerInbox.NEW_ORDER, newOrderStatuses);
        long newOrder = records.countSellerInbox(recipientId, SellerInbox.NEW_ORDER,
                RecordStatus.UNREAD, SellerInbox.NEW_ORDER, newOrderStatuses);
        long cancelledOrder = records.countSellerInbox(recipientId, SellerInbox.CANCELLED_ORDER,
                RecordStatus.UNREAD, SellerInbox.NEW_ORDER, newOrderStatuses);
        return new SellerUnreadCountsResponse(
                systemNotice + newOrder + cancelledOrder, systemNotice, newOrder, cancelledOrder);
    }

    @Override @Transactional(readOnly=true)
    public OffsetPageResponse<RecordResponse> getSystemRecords(Integer loginMemberId, Integer page) {
        permissions.validateSystemAdmin(loginMemberId);
        Map<Integer, String> sellerNames = new HashMap<>();
        return OffsetPageResponse.from(records.findAllByOrderByRecordCreatedAtDescRecordIdDesc(systemRecordPageRequest(page))
                .map(record -> {
                    RecordResponse response = responseMapper.toResponse(record);
                    if (record.getMsgFunction() != null
                            && record.getMsgFunction().startsWith("SC")
                            && record.getMsgfromSellerId() != null) {
                        response.setStoreName(sellerNames.computeIfAbsent(
                                record.getMsgfromSellerId(), sellers::getSellerName));
                    }
                    return response;
                }));
    }

    private PageRequest systemRecordPageRequest(Integer requestedPage) {
        int page = requestedPage == null ? 0 : requestedPage;
        if (page < 0) {
            throw new IllegalArgumentException("page 不可小於 0");
        }
        // Repository 方法名稱已固定 recordCreatedAt、recordId 倒序；此處不可再加入相同 Sort，
        // 否則 Hibernate 會產生重複 ORDER BY，SQL Server 會拒絕執行。
        return PageRequest.of(page, 20);
    }

    private PageRequest pageRequest(Integer requestedPage) {
        int page = requestedPage == null ? 0 : requestedPage;
        if (page < 0) {
            throw new IllegalArgumentException("page 不可小於 0");
        }
        return PageRequest.of(page, 10,
                Sort.by(Sort.Order.desc("recordCreatedAt"), Sort.Order.desc("recordId")));
    }

    private SendEntity requireSend(Integer id) {
        return sends.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 Send：" + id));
    }

    private RecordEntity requireRecord(Integer id) {
        return records.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 Record：" + id));
    }

    private RecordResponse toDetailResponse(RecordEntity record) {
        RecordResponse response = responseMapper.toResponse(record);
        if (record.getMsgFunction() != null
                && record.getMsgFunction().startsWith("SC")
                && record.getMsgfromSellerId() != null) {
            SellerInfoResponse seller = ModuleDataMapper.seller(
                    sellers.getSeller(record.getMsgfromSellerId()));
            response.setStoreName(seller.getSellerName());
        }
        return response;
    }

    private List<Integer> distinct(List<Integer> ids) {
        return ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private String prefix(RecordEntity record) {
        return record.getMsgFunction().substring(0, 2);
    }
    private void requireRecipientType(SendEntity send,boolean seller){
        String p=send.getMsgFunction().substring(0,2);
        boolean allowed=seller?Set.of("OA","OS","AS").contains(p):Set.of("OA","OC","AC","SC").contains(p);
        if(!allowed)throw new IllegalArgumentException(p+" 不允許此收件人類型");
    }
    private MemberAuthResponse asMemberProfile(SellerInfoResponse seller){
        MemberAuthResponse x = new MemberAuthResponse();
        x.setMemberId(seller.getMemberId());
        x.setSellerId(seller.getSellerId());
        x.setEmail(seller.getEmail());
        x.setEmailOrderNotifications(seller.getEmailOrderNotifications());
        x.setEmailMarketingNotifications(seller.getEmailMarketingNotifications());
        x.setAuthenticated(true);
        return x;
    }
}
