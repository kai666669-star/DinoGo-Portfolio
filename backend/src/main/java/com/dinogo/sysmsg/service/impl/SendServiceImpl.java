package com.dinogo.sysmsg.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.dto.request.send.*;
import com.dinogo.sysmsg.dto.request.template.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.member.service.MemberSysmsgProviderService;
//sysmsg-start，總共3次修改，第1次//
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.sysmsg.service.SysmsgImageService;
//sysmsg-end，總共3次修改，第1次//
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;
import java.util.NoSuchElementException;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;
import static com.dinogo.sysmsg.service.validation.SysmsgValidation.completeOrder;
import static com.dinogo.sysmsg.service.validation.SysmsgValidation.required;
import static com.dinogo.sysmsg.service.validation.SysmsgValidation.systemPrefix;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** 範本、手動發訊與 Send 狀態的交易邊界。 */
@Service
public class SendServiceImpl implements SendService {
    private final SendRepository sends;
    private final RecordService records;
    private final TemplateNumService numbers;
    private final MsgPermissionService permissions;
    private final OrderSysmsgProviderService orders; //Client-sysmsg
    private final MemberSysmsgProviderService members; //Client-sysmsg
    private final SellerSysmsgProviderService sellers; //Client-sysmsg
    private final ApplicationEventPublisher events;
    private final SendResponseMapper responseMapper;
    //sysmsg-start，總共3次修改，第3次//
    private final SysmsgImageService sysmsgImages;

    public SendServiceImpl(SendRepository sends, RecordService records, TemplateNumService numbers,
            MsgPermissionService permissions, OrderSysmsgProviderService orders, //Client-sysmsg
            MemberSysmsgProviderService members, SellerSysmsgProviderService sellers, //Client-sysmsg
            ApplicationEventPublisher events, SendResponseMapper responseMapper,
            SysmsgImageService sysmsgImages) {
        this.sends = sends;
        this.records = records;
        this.numbers = numbers;
        this.permissions = permissions;
        this.orders = orders;
        this.members = members;
        this.sellers = sellers;
        this.events = events;
        this.responseMapper = responseMapper;
        this.sysmsgImages = sysmsgImages;
    }
    //sysmsg-end，總共3次修改，第3次//

    @Override @Transactional
    public SendTemplateResponse createSellerTemplate(SellerTemplateCreateRequest r, Integer loginId) {
        Integer sellerId=permissions.validateSeller(loginId);
        validateImageReferences(r.getImgOne(), r.getImgOnePublicId(),
                r.getImgTwo(), r.getImgTwoPublicId(), r.getImgThree(), r.getImgThreePublicId(), loginId);
        SendSellerEntity entity=new SendSellerEntity(sellerId,numbers.generateMsgFunction("SC"),label(r.getMsgLabel(),r.getSendTitle()),
                required(r.getSendTitle(),"標題"),required(r.getSendContent(),"內容"),SendStatus.SAVE,
                null,r.getImgOne(),r.getImgTwo(),r.getImgThree(),r.getSendRemark());
        setImagePublicIds(entity, r.getImgOnePublicId(), r.getImgTwoPublicId(), r.getImgThreePublicId());
        return responseMapper.toTemplateResponse(sends.save(entity));
    }

    @Override @Transactional
    public SendTemplateResponse createSystemTemplate(SysTemplateCreateRequest r, Integer loginId) {
        permissions.validateSystemAdmin(loginId); String prefix=systemPrefix(r.getMsgType());
        return responseMapper.toTemplateResponse(sends.save(base(1,numbers.generateMsgFunction(prefix),r.getMsgLabel(),r.getSendTitle(),r.getSendContent(),SendStatus.SAVE)));
    }

    @Override @Transactional
    public SendTemplateResponse updateTemplate(Integer sendId, SendTemplateUpdateRequest r, Integer loginId) {
        validateImageReferences(r.getImgOne(), r.getImgOnePublicId(),
                r.getImgTwo(), r.getImgTwoPublicId(), r.getImgThree(), r.getImgThreePublicId(), loginId);
        SendEntity old=require(sendId); requireStatus(old,SendStatus.SAVE); permissions.validateTemplateOwner(old,loginId);
        if (!(old instanceof SendSellerEntity) && r.getMsgType() != null && !r.getMsgType().isBlank()) {
            String targetPrefix=systemPrefix(r.getMsgType());
            if (!targetPrefix.equals(prefix(old))) {
                SendEntity replacement=base(old.getMsgfromSellerId(),numbers.generateMsgFunction(targetPrefix),r.getMsgLabel(),r.getSendTitle(),r.getSendContent(),SendStatus.SAVE);
                sends.delete(old);
                return responseMapper.toTemplateResponse(sends.save(replacement));
            }
        }
        old.setMsgLabel(label(r.getMsgLabel(),r.getSendTitle())); old.setSendTitle(required(r.getSendTitle(),"標題")); old.setSendContent(required(r.getSendContent(),"內容"));
        if (old instanceof SendSellerEntity sc) {
            sc.setImgOne(r.getImgOne());
            sc.setImgOnePublicId(r.getImgOnePublicId());
            sc.setImgTwo(r.getImgTwo());
            sc.setImgTwoPublicId(r.getImgTwoPublicId());
            sc.setImgThree(r.getImgThree());
            sc.setImgThreePublicId(r.getImgThreePublicId());
            sc.setSendRemark(r.getSendRemark());
        }
        return responseMapper.toTemplateResponse(sends.save(old));
    }

    @Override @Transactional
    public SendResponse applySystemTemplate(SysTemplateApplyRequest r,Integer loginId){
        SendEntity t=require(r.getSendId()); requireStatus(t,SendStatus.SAVE); permissions.validateTemplateOwner(t,loginId);
        String p=systemPrefix(prefix(t)); SendEntity actual=copy(t,t.getMsgFunction(),SendStatus.SEND); actual=sends.save(actual);
        createSystemRecords(actual, p, r.getMsgtoMemberId(), r.getMsgtoSellerId());
        return responseMapper.toResponse(actual);
    }

    @Override @Transactional
    public SendResponse applySellerTemplate(SellerTemplateApplyRequest r,Integer loginId){
        Integer sellerId=permissions.validateSeller(loginId); SendEntity t=require(r.getSendId()); requireStatus(t,SendStatus.SAVE); permissions.validateTemplateOwner(t,loginId);
        if(!(t instanceof SendSellerEntity sc)||!"SC".equals(prefix(t)))throw new IllegalStateException("只能套用 SC 範本");
        OrderInfoResponse order=completeOrder(ModuleDataMapper.order(orders.getOrder(r.getOrderId()))); requireOrderOwner(order,sellerId); //Client-sysmsg
        SendSellerEntity actual=new SendSellerEntity(sellerId,t.getMsgFunction(),t.getMsgLabel(),t.getSendTitle(),t.getSendContent(),SendStatus.SEND,order.getOrderNo(),sc.getImgOne(),sc.getImgTwo(),sc.getImgThree(),sc.getSendRemark());
        setImagePublicIds(actual, sc.getImgOnePublicId(), sc.getImgTwoPublicId(), sc.getImgThreePublicId());
        sends.save(actual);
        records.createSingleMemberRecord(actual.getSendId(), order.getBuyerId());
        return responseMapper.toResponse(actual);
    }

    @Override @Transactional
    public SendResponse createSellerSend(SellerCreateRequest r,Integer loginId){
        validateImageReferences(r.getImgOne(), r.getImgOnePublicId(),
                r.getImgTwo(), r.getImgTwoPublicId(), r.getImgThree(), r.getImgThreePublicId(), loginId);
        Integer sellerId=permissions.validateSeller(loginId); OrderInfoResponse order=completeOrder(ModuleDataMapper.order(orders.getOrder(r.getOrderId()))); requireOrderOwner(order,sellerId); //Client-sysmsg
        SendSellerEntity actual=new SendSellerEntity(sellerId,numbers.generateMsgFunction("SC"),r.getSendTitle(),r.getSendTitle(),r.getSendContent(),SendStatus.SEND,order.getOrderNo(),r.getImgOne(),r.getImgTwo(),r.getImgThree(),r.getSendRemark());
        setImagePublicIds(actual, r.getImgOnePublicId(), r.getImgTwoPublicId(), r.getImgThreePublicId());
        sends.save(actual);
        records.createSingleMemberRecord(actual.getSendId(), order.getBuyerId());
        return responseMapper.toResponse(actual);
    }

    @Override @Transactional
    public SendResponse createSystemSend(SysCreateRequest r,Integer loginId){
        permissions.validateSystemAdmin(loginId); String p=systemPrefix(r.getMsgType());
        SendEntity actual=sends.save(base(1,numbers.generateMsgFunction(p),null,r.getSendTitle(),r.getSendContent(),SendStatus.SEND));
        createSystemRecords(actual, p, r.getMsgtoMemberId(), r.getMsgtoSellerId());
        return responseMapper.toResponse(actual);
    }

    //sysmsg-start，總共3次修改，第2次//
    /** 功能：阻擋客戶端繞過上傳 API 寫入任意網址；應用：附件來源固定為 Cloudinary。 */
    private void validateImageReferences(String url1, String id1, String url2, String id2,
            String url3, String id3, Integer memberId) {
        String ownerPrefix = "dinogo/sysmsg/" + memberId;
        sysmsgImages.validateReference(url1, id1, ownerPrefix);
        sysmsgImages.validateReference(url2, id2, ownerPrefix);
        sysmsgImages.validateReference(url3, id3, ownerPrefix);
    }

    private void setImagePublicIds(SendSellerEntity entity, String id1, String id2, String id3) {
        entity.setImgOnePublicId(id1);
        entity.setImgTwoPublicId(id2);
        entity.setImgThreePublicId(id3);
    }
    //sysmsg-end，總共3次修改，第2次//

    @Override @Transactional
    public SendResponse changeSendStatus(Integer id,SendStatusUpdateRequest r,Integer loginId){
        SendEntity s=require(id); permissions.validateTemplateOwner(s,loginId); SendStatus target=Objects.requireNonNull(r.getTargetStatus(),"targetStatus");
        boolean legal=(s.getSendStatus()==SendStatus.SAVE&&(target==SendStatus.SEND||target==SendStatus.DELETE))||(s.getSendStatus()==SendStatus.SEND&&target==SendStatus.DELETE);
        if(!legal)throw new IllegalStateException("不合法的 Send 狀態轉換："+s.getSendStatus()+" → "+target);
        if(target==SendStatus.SEND&&records.existsBySendId(id))throw new IllegalStateException("SAVE 範本本身不能成為已存在 Record 的 SEND");
        s.setSendStatus(target);
        return responseMapper.toResponse(sends.save(s));
    }
    @Override
    @Transactional
    public void deleteSend(Integer id, Integer loginId) {
        SendStatusUpdateRequest request = new SendStatusUpdateRequest();
        request.setTargetStatus(SendStatus.DELETE);
        changeSendStatus(id, request, loginId);
    }
    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<SendTemplateResponse> findSystemTemplates(
            Integer loginId, Integer page) {
        permissions.validateSystemAdmin(loginId);
        return OffsetPageResponse.from(sends.findSystemTemplates(
                SendStatus.SAVE, pageRequest(page)).map(responseMapper::toTemplateResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<SendTemplateResponse> findSellerTemplates(
            Integer loginId, Integer page) {
        Integer sellerId = permissions.validateSeller(loginId);
        return findSendPage(sellerId, SendStatus.SAVE, "SC", page,
                responseMapper::toTemplateResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<SendResponse> findSellerOutbox(Integer loginId, Integer page) {
        Integer sellerId = permissions.validateSeller(loginId);
        return findSendPage(sellerId, SendStatus.SEND, "SC", page, responseMapper::toResponse);
    }

    private <T> OffsetPageResponse<T> findSendPage(Integer sellerId, SendStatus status,
            String prefix, Integer page,
            java.util.function.Function<SendEntity, T> mapper) {
        return OffsetPageResponse.from(sends.findBySenderAndStatus(
                sellerId, status, prefix, pageRequest(page)).map(mapper));
    }

    private PageRequest pageRequest(Integer requestedPage) {
        int page = requestedPage == null ? 0 : requestedPage;
        if (page < 0) {
            throw new IllegalArgumentException("page 不可小於 0");
        }
        return PageRequest.of(page, 10,
                Sort.by(Sort.Order.desc("sendUpdAt"), Sort.Order.desc("sendId")));
    }

    private SendEntity base(Integer from, String function, String label, String title,
            String content, SendStatus status) {
        return new SendEntity(from, function, label(label, title), required(title, "標題"),
                required(content, "內容"), status);
    }

    private SendEntity copy(SendEntity template, String function, SendStatus status) {
        return base(template.getMsgfromSellerId(), function, template.getMsgLabel(),
                template.getSendTitle(), template.getSendContent(), status);
    }

    private SendEntity require(Integer id) {
        return sends.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 Send：" + id));
    }

    private void requireStatus(SendEntity send, SendStatus status) {
        if (send.getSendStatus() != status) {
            throw new IllegalStateException("Send 必須為 " + status);
        }
    }

    private String prefix(SendEntity send) {
        return send.getMsgFunction().substring(0, 2);
    }

    private String label(String value, String title) {
        return value == null || value.isBlank() ? required(title, "標題") : value.trim();
    }
    private void createSystemRecords(SendEntity send,String p,Integer memberId,Integer sellerId){
        if ("OA".equals(p)) {
            // OA 預設為全體廣播；管理員若輸入個別會員 ID，則只建立該會員的 Record。
            if (memberId != null) {
                records.createSingleMemberRecord(send.getSendId(), memberId);
                return;
            }
            events.publishEvent(new OaBroadcastRequested(send.getSendId()));
            return;
        }
        records.createRecords(send.getSendId(),memberRecipients(p,memberId),sellerRecipients(p,sellerId));
    }
    private List<Integer> memberRecipients(String p,Integer id){
        if("OS".equals(p))return List.of();
        if("OA".equals(p))return List.of();
        if(id==null)return members.getAllMembers().stream().map(member->member.memberId()).distinct().toList(); //Client-sysmsg
        members.getMember(id); //Client-sysmsg
        return List.of(id);
    }
    private List<Integer> sellerRecipients(String p,Integer id){
        if("OC".equals(p))return List.of();
        if("OA".equals(p))return List.of();
        if(id==null)return sellers.getAllSellers().stream().filter(seller->seller.active()).map(seller->seller.sellerId()).distinct().toList(); //Client-sysmsg
        var seller=sellers.getAllSellers().stream().filter(candidate->id.equals(candidate.memberId())||id.equals(candidate.sellerId())).findFirst()
                .orElseThrow(()->new NoSuchElementException("找不到商家會員："+id)); //Client-sysmsg
        if(!seller.active())throw new IllegalStateException("收件商家未啟用："+seller.sellerId());
        return List.of(seller.sellerId());
    }
    private void requireOrderOwner(OrderInfoResponse order, Integer sellerId) {
        if (!sellerId.equals(order.getSellerId())) {
            throw new SecurityException("該商家無權使用此訂單");
        }
    }
}
