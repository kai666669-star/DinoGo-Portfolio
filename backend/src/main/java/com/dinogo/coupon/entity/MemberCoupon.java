package com.dinogo.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(schema = "seller", name = "MemberCoupon")
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Integer memberCouponId;

    @Column(name = "coupon_id", nullable = false)
    private Integer couponId;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Column(name = "is_used", nullable = false)
    private Boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public void setMemberCouponId(Integer memberCouponId) {
        this.memberCouponId = memberCouponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public Integer getMemberCouponId() {
        return memberCouponId;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public Boolean getUsed() {
        return used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

}
