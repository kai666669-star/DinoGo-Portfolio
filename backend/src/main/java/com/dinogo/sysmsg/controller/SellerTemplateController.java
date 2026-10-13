package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.template.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.service.SendService;

@RestController
@RequestMapping("/api/sysmsg/seller/templates")
public class SellerTemplateController {
    private final SendService service; private final ControllerSupport auth;
    public SellerTemplateController(SendService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SendTemplateResponse create(@Valid @RequestBody SellerTemplateCreateRequest request){return service.createSellerTemplate(request,auth.memberId());}
    @PutMapping("/{sendId}") public SendTemplateResponse update(@PathVariable Integer sendId,@Valid @RequestBody SendTemplateUpdateRequest request){return service.updateTemplate(sendId,request,auth.memberId());}
    @PostMapping("/{sendId}/apply") @ResponseStatus(HttpStatus.CREATED)
    public SendResponse apply(@PathVariable Integer sendId,@RequestBody SellerTemplateApplyRequest request){request.setSendId(sendId);return service.applySellerTemplate(request,auth.memberId());}
    @GetMapping
    public OffsetPageResponse<SendTemplateResponse> findAll(
            @RequestParam(defaultValue = "0") Integer page) {
        return service.findSellerTemplates(auth.memberId(), page);
    }
    @DeleteMapping("/{sendId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer sendId){service.deleteSend(sendId,auth.memberId());}
}
