package com.dinogo.member.dto;
import java.time.LocalDateTime;
import java.util.List;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
public record AdminMemberResponse(Integer memberId, String email, String lastName, String firstName, List<String> roles, String status, LocalDateTime createdAt) {
    public static AdminMemberResponse from(Member member) {
        List<String> roles = member.getMemberRoles().stream().map(MemberRole::getRole).map(role -> role.getRoleName()).sorted().toList();
        return new AdminMemberResponse(member.getMemberId(), member.getEmail(), member.getLastName(), member.getFirstName(), roles, member.getStatus(), member.getCreatedAt());
    }
}
