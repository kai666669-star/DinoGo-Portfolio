package com.dinogo.member.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "Address", schema = "member")
public class Address {

    // 地址主鍵。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    // 地址所屬會員。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties({ "addresses", "memberRoles" })
    private Member member;

    // 收件人資料。
    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    // 地址內容。
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "city", nullable = false, length = 10)
    private String city;

    @Column(name = "district", nullable = false, length = 10)
    private String district;

    @Column(name = "detail_address", nullable = false, length = 255)
    private String detailAddress;

    // 是否為會員目前的預設地址。
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    // 避免 toString 輸出會員關聯與完整個資。
    @Override
    public String toString() {
        return "Address [addressId=" + addressId + ", isDefault=" + isDefault + "]";
    }

    // 主鍵與會員關聯存取方法。
    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    // 收件人資料存取方法。
    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    // 地址內容存取方法。
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    // 預設地址狀態存取方法。
    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    // 未指定 isDefault 時使用資料庫規格的 false 預設值。
    @PrePersist
    protected void onCreate() {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
