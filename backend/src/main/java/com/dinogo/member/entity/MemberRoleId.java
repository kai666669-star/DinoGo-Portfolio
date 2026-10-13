package com.dinogo.member.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MemberRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "member_id")
    private Integer memberId;

    @Column(name = "role_id")
    private Integer roleId;

    public MemberRoleId() {
    }

    public MemberRoleId(Integer memberId, Integer roleId) {
        this.memberId = memberId;
        this.roleId = roleId;
    }

    @Override
    public String toString() {
        return "MemberRoleId [memberId=" + memberId + ", roleId=" + roleId + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, roleId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MemberRoleId other = (MemberRoleId) obj;
        return Objects.equals(memberId, other.memberId) && Objects.equals(roleId, other.roleId);
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
