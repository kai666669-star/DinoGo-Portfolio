package com.dinogo.member.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.AddressRequest;
import com.dinogo.member.dto.AddressResponse;
import com.dinogo.member.entity.Address;
import com.dinogo.member.entity.Member;
import com.dinogo.member.exception.AddressInUseException;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.member.repository.MemberRepository;

/** 地址 CRUD 與預設地址規則集中在此 Service。 */
@Service
public class AddressService {

    // 地址與會員資料存取。
    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;

    /** 使用 constructor injection 取得必要的 Repository。 */
    public AddressService(AddressRepository addressRepository, MemberRepository memberRepository) {
        this.addressRepository = addressRepository;
        this.memberRepository = memberRepository;
    }

    /** 查詢登入會員的地址，預設地址會排在第一筆。 */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Integer memberId) {
        return addressRepository
                .findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(memberId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    /** 查詢登入會員擁有的單筆地址。 */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Integer memberId, Integer addressId) {
        return AddressResponse.from(findOwnedAddress(memberId, addressId));
    }

    /** 建立地址；第一筆地址一定會成為預設地址。 */
    @Transactional
    public AddressResponse createAddress(Integer memberId, AddressRequest request) {
        Member member = lockMemberForAddressWrite(memberId);

        // 使用者指定新預設地址時，先取消目前的預設地址。
        boolean isFirstAddress = !addressRepository.existsByMemberMemberId(memberId);
        boolean wantsDefault = Boolean.TRUE.equals(request.isDefault());
        if (wantsDefault) {
            clearDefaultAddresses(memberId, null);
            // SQL Server 的 filtered unique index 要先看見舊預設已取消。
            addressRepository.flush();
        }

        // 將 request 欄位寫入新 Entity。
        Address address = new Address();
        address.setMember(member);
        applyRequest(address, request);
        address.setIsDefault(isFirstAddress || wantsDefault);

        return AddressResponse.from(addressRepository.save(address));
    }

    /** 修改地址；取消預設時會將另一筆地址設為新預設。 */
    @Transactional
    public AddressResponse updateAddress(Integer memberId, Integer addressId, AddressRequest request) {
        lockMemberForAddressWrite(memberId);
        Address address = findOwnedAddress(memberId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        Boolean requestedDefault = request.isDefault();

        // 設定新預設地址時，取消其他地址的預設狀態。
        if (Boolean.TRUE.equals(requestedDefault)) {
            clearDefaultAddresses(memberId, addressId);
            // 在將此地址設為預設前，先寫入其他地址的 false。
            addressRepository.flush();
        }

        // 更新一般地址欄位。
        applyRequest(address, request);

        // null 代表維持原狀，只有明確傳入 true 或 false 才調整預設狀態。
        if (requestedDefault != null) {
            address.setIsDefault(requestedDefault);

            // 唯一預設被取消時，優先把其他第一筆設為預設；沒有其他地址則保留原預設。
            if (wasDefault
                    && !requestedDefault
                    && !hasAnotherAddress(memberId, addressId)) {
                address.setIsDefault(true);
            } else if (wasDefault && !requestedDefault) {
                // 先取消目前預設並寫入，再提升另一筆，避免 unique index 2601。
                addressRepository.flush();
                promoteAnotherDefaultAddress(memberId, addressId);
            }
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    /** 刪除地址；已被歷史訂單引用時轉成可辨識的業務例外。 */
    @Transactional
    public void deleteAddress(Integer memberId, Integer addressId) {
        lockMemberForAddressWrite(memberId);
        Address address = findOwnedAddress(memberId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        // flush 讓 FK 衝突在 Service 交易內發生，才能轉成 409 回應。
        try {
            addressRepository.delete(address);
            addressRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new AddressInUseException("此地址已被訂單使用，無法刪除", exception);
        }

        // 刪除預設地址後，將剩餘清單第一筆設為新的預設地址。
        if (wasDefault) {
            promoteAnotherDefaultAddress(memberId, addressId);
        }
    }

    /** 同時使用 addressId 與 memberId 查詢，避免跨會員存取。 */
    private Address findOwnedAddress(Integer memberId, Integer addressId) {
        return addressRepository
                .findByAddressIdAndMemberMemberId(addressId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
    }

    /** 取消其他地址的預設狀態。 */
    private void clearDefaultAddresses(Integer memberId, Integer excludedAddressId) {
        addressRepository
                .findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(memberId)
                .stream()
                .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                .filter(address -> excludedAddressId == null
                        || !excludedAddressId.equals(address.getAddressId()))
                .forEach(address -> address.setIsDefault(false));
    }

    /** 將目前地址以外的第一筆設為預設，並回傳是否找到候選地址。 */
    private boolean promoteAnotherDefaultAddress(Integer memberId, Integer excludedAddressId) {
        return addressRepository
                .findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(memberId)
                .stream()
                .filter(address -> !excludedAddressId.equals(address.getAddressId()))
                .findFirst()
                .map(nextAddress -> {
                    nextAddress.setIsDefault(true);
                    addressRepository.save(nextAddress);
                    return true;
                })
                .orElse(false);
    }

    /** Locks the member row so one member's address writes cannot race on the default-address index. */
    private Member lockMemberForAddressWrite(Integer memberId) {
        return memberRepository.findByIdForAddressWrite(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }

    private boolean hasAnotherAddress(Integer memberId, Integer excludedAddressId) {
        return addressRepository
                .findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(memberId)
                .stream()
                .anyMatch(address -> !excludedAddressId.equals(address.getAddressId()));
    }

    /** 將 request 中可編輯的欄位套用到 Address Entity。 */
    private void applyRequest(Address address, AddressRequest request) {
        address.setReceiverName(request.receiverName());
        address.setReceiverPhone(request.receiverPhone());
        address.setPostalCode(request.postalCode());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetailAddress(request.detailAddress());
    }
}
