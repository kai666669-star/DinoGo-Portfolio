package com.dinogo.seller.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.dinogo.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(schema = "seller", name = "Seller")
public class Seller {

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public void setStoreLogoUrl(String storeLogoUrl) {
        this.storeLogoUrl = storeLogoUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_id")
    private Integer sellerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "store_description")
    private String storeDescription;

    @Column(name = "store_logo_url")
    private String storeLogoUrl;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LocalTime getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(LocalTime serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public LocalTime getServiceEndTime() {
        return serviceEndTime;
    }

    public void setServiceEndTime(LocalTime serviceEndTime) {
        this.serviceEndTime = serviceEndTime;
    }

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "service_start_time")
    private LocalTime serviceStartTime;

    @Column(name = "service_end_time")
    private LocalTime serviceEndTime;

    public Integer getSellerId() {
        return sellerId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Integer getMemberId() {
        return member == null ? null : member.getMemberId();
    }

    public String getStoreName() {
        return storeName;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public String getStoreLogoUrl() {
        return storeLogoUrl;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
