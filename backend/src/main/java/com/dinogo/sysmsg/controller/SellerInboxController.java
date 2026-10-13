package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.entity.RecordStatus;
import com.dinogo.sysmsg.dto.request.record.RecordStatusUpdateRequest;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.service.RecordService;

@RestController
@RequestMapping("/api/sysmsg/seller/inbox")
public class SellerInboxController {
    private final RecordService service; private final ControllerSupport auth;
    public SellerInboxController(RecordService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    //msg-平台公告// 前端以 category=SYSTEM_NOTICE 取得平台公告收件匣。
    //msg-訂單進度// 前端以 category=NEW_ORDER 取得訂單進度收件匣。
    //msg-取消訂單// 前端以 category=CANCELLED_ORDER 取得取消訂單收件匣。
    @GetMapping
    public OffsetPageResponse<MsgInboxResponse> inbox(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") Integer page) {
        return service.getSellerInbox(auth.sellerId(), category, page);
    }
    @GetMapping("/unread-counts")
    public SellerUnreadCountsResponse unreadCounts() {
        return service.countSellerUnread(auth.sellerId());
    }
    //msg-訊息title// 詳情端點回傳 Send 的完整標題。
    //msg-訊息content// 詳情端點回傳 Send 的完整內容。
    @GetMapping("/{recordId}") public RecordResponse get(@PathVariable Integer recordId){return service.searchRecord(recordId,auth.memberId());}
    @PatchMapping("/{recordId}/status") public RecordResponse status(@PathVariable Integer recordId,@Valid @RequestBody RecordStatusUpdateRequest request){
        if(request.getTargetStatus()!=RecordStatus.READ)throw new IllegalArgumentException("收件匣狀態端點只允許 UNREAD → READ");
        return service.readRecord(recordId,auth.memberId());
    }
    @DeleteMapping("/{recordId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer recordId){service.deleteRecord(recordId,auth.memberId());}
}
