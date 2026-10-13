package com.dinogo.sysmsg.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.template.*;
import com.dinogo.sysmsg.dto.response.*;
import com.dinogo.sysmsg.service.SendService;

@RestController
@RequestMapping("/api/sysmsg/system/templates")
public class SystemTemplateController {
    private final SendService service; private final ControllerSupport auth;
    public SystemTemplateController(SendService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SendTemplateResponse create(@Valid @RequestBody SysTemplateCreateRequest request){return service.createSystemTemplate(request,auth.memberId());}
    @PutMapping("/{sendId}") public SendTemplateResponse update(@PathVariable Integer sendId,@Valid @RequestBody SendTemplateUpdateRequest request){return service.updateTemplate(sendId,request,auth.memberId());}
    @PostMapping("/{sendId}/apply") @ResponseStatus(HttpStatus.CREATED)
    public SendResponse apply(@PathVariable Integer sendId,@RequestBody SysTemplateApplyRequest request){request.setSendId(sendId);return service.applySystemTemplate(request,auth.memberId());}
    @GetMapping
    public OffsetPageResponse<SendTemplateResponse> findAll(
            @RequestParam(defaultValue = "0") Integer page) {
        return service.findSystemTemplates(auth.memberId(), page);
    }
    @DeleteMapping("/{sendId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer sendId){service.deleteSend(sendId,auth.memberId());}
}
