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
@RequestMapping("/api/sysmsg/member/inbox")
public class MemberInboxController {
    private final RecordService service; private final ControllerSupport auth;
    public MemberInboxController(RecordService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    //msg-系統通知// 前端以 category=SYSTEM_INBOX 取得系統通知標籤收件匣。
    //msg-訂單通知// 前端以 category=ORDER_INBOX 取得訂單通知標籤收件匣。
    //msg-賣家通知// 前端以 category=SELLER_INBOX 取得賣家通知標籤收件匣。
    @GetMapping
    public OffsetPageResponse<MsgInboxResponse> inbox(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") Integer page) {
        return service.getMemberInbox(auth.memberId(), category, page);
    }
    //msg-首頁通知未讀// 首頁通知紅色圓點的未讀數量 API。
    @GetMapping("/unread-count") public UnreadCountResponse unreadCount(){
        return new UnreadCountResponse(service.countMemberUnread(auth.memberId()));
    }
    //msg-訊息msgfrom// 詳情端點由 Record 關聯的 Send 回傳系統或商家寄件來源。
    //msg-訊息title// 詳情端點回傳 Send 的完整標題。
    //msg-訊息content// 詳情端點回傳 Send 的完整內容。
    @GetMapping("/{recordId}") public RecordResponse get(@PathVariable Integer recordId){return service.searchRecord(recordId,auth.memberId());}
    //msg-已讀回傳後端// 會員開啟詳情後呼叫此端點，僅允許 UNREAD → READ。
    @PatchMapping("/{recordId}/status") public RecordResponse status(@PathVariable Integer recordId,@Valid @RequestBody RecordStatusUpdateRequest request){
        if(request.getTargetStatus()!=RecordStatus.READ)throw new IllegalArgumentException("收件匣狀態端點只允許 UNREAD → READ");
        return service.readRecord(recordId,auth.memberId());
    }
    @DeleteMapping("/{recordId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer recordId){service.deleteRecord(recordId,auth.memberId());}
}
