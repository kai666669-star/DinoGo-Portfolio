package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dinogo.sysmsg.dto.request.send.SellerCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.OffsetPageResponse;
import com.dinogo.sysmsg.dto.response.SysmsgImageUploadResponse;
import com.dinogo.sysmsg.service.SendService;
import com.dinogo.sysmsg.service.SysmsgImageService;

//sysmsg-start，總共2次修改，第1次//
/** 商家訊息寄送、寄件備份查詢與寄件備份軟刪除整合端口。 */
@RestController
@RequestMapping("/api/sysmsg/seller/messages")
public class SellerMessageController {
    private final SendService service;
    private final ControllerSupport auth;
    private final SysmsgImageService imageService;
    public SellerMessageController(SendService service, ControllerSupport auth, SysmsgImageService imageService) {
        this.service = service; this.auth = auth; this.imageService = imageService;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SendResponse create(@Valid @RequestBody SellerCreateRequest request) { return service.createSellerSend(request, auth.memberId()); }
    @GetMapping("/outbox")
    public OffsetPageResponse<SendResponse> outbox(
            @RequestParam(defaultValue = "0") Integer page) {
        return service.findSellerOutbox(auth.memberId(), page);
    }
    @DeleteMapping("/outbox/{sendId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOutbox(@PathVariable Integer sendId) { service.deleteSend(sendId, auth.memberId()); }
    //sysmsg-end，總共2次修改，第1次//

    //sysmsg-start，總共2次修改，第2次//
    /** 圖片先上傳 Cloudinary，建立訊息時僅送回傳的 URL。 */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SysmsgImageUploadResponse uploadImages(@RequestParam("files") List<MultipartFile> files) {
        return new SysmsgImageUploadResponse(imageService.upload(files, auth.memberId()));
    }
    //sysmsg-end，總共2次修改，第2次//
}
