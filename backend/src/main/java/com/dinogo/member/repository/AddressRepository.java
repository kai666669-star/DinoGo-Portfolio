package com.dinogo.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    // 保留給既有模組使用的一般會員地址查詢。
    public List<Address> findByMemberMemberId(Integer memberId);

    // 地址管理與結帳頁使用，預設地址固定排在前面。
    public List<Address> findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(Integer memberId);

    // 驗證地址 ownership，避免跨會員存取。
    public Optional<Address> findByAddressIdAndMemberMemberId(Integer addressId, Integer memberId);

    // 保留給既有預設地址查詢使用。
    public Optional<Address> findByMemberMemberIdAndIsDefaultTrue(Integer memberId);

    // 建立地址時判斷是否為會員第一筆地址。
    public boolean existsByMemberMemberId(Integer memberId);
}
