package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dinogo.member.dto.MemberStatusChangeRequest;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.service.MemberAccountService;
import com.dinogo.security.AuthenticatedMember;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {
    private final MemberAccountService memberAccountService;
    public AdminMemberController(MemberAccountService memberAccountService) { this.memberAccountService = memberAccountService; }
    @GetMapping public Object list(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword) { return memberAccountService.listMembers(status, keyword); }
    @PostMapping("/{memberId}/suspend") public ResponseEntity<?> suspend(@PathVariable Integer memberId, @AuthenticationPrincipal AuthenticatedMember admin, @Valid @RequestBody MemberStatusChangeRequest request) { return change(() -> memberAccountService.suspend(memberId, admin.memberId(), request.reason())); }
    @PostMapping("/{memberId}/restore") public ResponseEntity<?> restore(@PathVariable Integer memberId, @AuthenticationPrincipal AuthenticatedMember admin) { return change(() -> memberAccountService.restore(memberId, admin.memberId())); }
    private ResponseEntity<?> change(java.util.concurrent.Callable<?> action) { try { return ResponseEntity.ok(action.call()); } catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().body(MemberApiErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage())); } catch (Exception exception) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MemberApiErrorResponse.from(HttpStatus.INTERNAL_SERVER_ERROR, "系統暫時無法處理請求")); } }
}
