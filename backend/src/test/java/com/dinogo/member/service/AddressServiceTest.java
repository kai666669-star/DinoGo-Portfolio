package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.dinogo.member.dto.AddressRequest;
import com.dinogo.member.dto.AddressResponse;
import com.dinogo.member.entity.Address;
import com.dinogo.member.entity.Member;
import com.dinogo.member.exception.AddressInUseException;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    // Service 依賴與共用測試資料。
    @Mock
    private AddressRepository addressRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AddressService addressService;

    private Member member;
    private AddressRequest request;

    // 每個測試使用同一組基本會員與地址輸入。
    @BeforeEach
    void setUp() {
        member = new Member();
        member.setMemberId(1);

        request = new AddressRequest(
                "王小明",
                "0912345678",
                "100",
                "台北市",
                "中正區",
                "忠孝西路一段 1 號",
                false);
    }

    // 查詢與建立地址測試。
    @Test
    void getAddressesReturnsOnlyAuthenticatedMemberAddresses() {
        Address defaultAddress = address(1, true);
        Address secondAddress = address(2, false);
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(defaultAddress, secondAddress));

        List<AddressResponse> responses = addressService.getAddresses(1);

        assertThat(responses).extracting(AddressResponse::addressId)
                .containsExactly(1, 2);
        assertThat(responses.getFirst().isDefault()).isTrue();
        verify(addressRepository).findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1);
    }

    @Test
    void createAddressMakesFirstAddressDefault() {
        mockAddressWriteMember();
        when(addressRepository.existsByMemberMemberId(1)).thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setAddressId(1);
            return address;
        });

        AddressResponse response = addressService.createAddress(1, request);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isEqualTo(member);
        assertThat(captor.getValue().getIsDefault()).isTrue();
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void createDefaultAddressClearsPreviousDefault() {
        mockAddressWriteMember();
        Address previousDefault = address(1, true);
        AddressRequest defaultRequest = request(true);
        when(addressRepository.existsByMemberMemberId(1)).thenReturn(true);
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(previousDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.createAddress(1, defaultRequest);

        assertThat(previousDefault.getIsDefault()).isFalse();
        assertThat(response.isDefault()).isTrue();
        InOrder inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).flush();
        inOrder.verify(addressRepository).save(any(Address.class));
    }

    // 修改預設地址規則測試。
    @Test
    void updateDefaultAddressToFalsePromotesAnotherAddress() {
        mockAddressWriteMember();
        Address currentDefault = address(1, true);
        Address nextAddress = address(2, false);
        when(addressRepository.findByAddressIdAndMemberMemberId(1, 1))
                .thenReturn(Optional.of(currentDefault));
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(currentDefault, nextAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.updateAddress(1, 1, request);

        assertThat(response.isDefault()).isFalse();
        assertThat(nextAddress.getIsDefault()).isTrue();
        verify(addressRepository).flush();
        verify(addressRepository).save(nextAddress);
    }

    @Test
    void updateOnlyDefaultAddressToFalseKeepsItDefault() {
        mockAddressWriteMember();
        Address currentDefault = address(1, true);
        when(addressRepository.findByAddressIdAndMemberMemberId(1, 1))
                .thenReturn(Optional.of(currentDefault));
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(currentDefault));
        when(addressRepository.save(currentDefault)).thenReturn(currentDefault);

        AddressResponse response = addressService.updateAddress(1, 1, request);

        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void updateAddressWithoutIsDefaultKeepsCurrentDefaultState() {
        mockAddressWriteMember();
        Address currentDefault = address(1, true);
        AddressRequest requestWithoutDefault = new AddressRequest(
                "王小明",
                "0912345678",
                "100",
                "台北市",
                "中正區",
                "忠孝西路一段 1 號",
                null);
        when(addressRepository.findByAddressIdAndMemberMemberId(1, 1))
                .thenReturn(Optional.of(currentDefault));
        when(addressRepository.save(currentDefault)).thenReturn(currentDefault);

        AddressResponse response = addressService.updateAddress(1, 1, requestWithoutDefault);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository, never())
                .findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1);
    }

    @Test
    void updateAddressToDefaultClearsOtherDefaults() {
        mockAddressWriteMember();
        Address previousDefault = address(1, true);
        Address updatedAddress = address(2, false);
        when(addressRepository.findByAddressIdAndMemberMemberId(2, 1))
                .thenReturn(Optional.of(updatedAddress));
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(previousDefault, updatedAddress));
        when(addressRepository.save(updatedAddress)).thenReturn(updatedAddress);

        AddressResponse response = addressService.updateAddress(1, 2, request(true));

        assertThat(previousDefault.getIsDefault()).isFalse();
        assertThat(response.isDefault()).isTrue();
        InOrder inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).flush();
        inOrder.verify(addressRepository).save(updatedAddress);
    }

    // 刪除與資料完整性測試。
    @Test
    void deleteDefaultAddressPromotesNextAddress() {
        mockAddressWriteMember();
        Address defaultAddress = address(1, true);
        Address nextAddress = address(2, false);
        when(addressRepository.findByAddressIdAndMemberMemberId(1, 1))
                .thenReturn(Optional.of(defaultAddress));
        when(addressRepository.findByMemberMemberIdOrderByIsDefaultDescAddressIdAsc(1))
                .thenReturn(List.of(nextAddress));

        addressService.deleteAddress(1, 1);

        verify(addressRepository).delete(defaultAddress);
        verify(addressRepository).flush();
        verify(addressRepository).save(nextAddress);
        assertThat(nextAddress.getIsDefault()).isTrue();
    }

    @Test
    void deleteAddressReferencedByOrderReturnsAddressInUseError() {
        mockAddressWriteMember();
        Address usedAddress = address(1, true);
        when(addressRepository.findByAddressIdAndMemberMemberId(1, 1))
                .thenReturn(Optional.of(usedAddress));
        doThrow(new DataIntegrityViolationException("FK violation"))
                .when(addressRepository).flush();

        assertThatThrownBy(() -> addressService.deleteAddress(1, 1))
                .isInstanceOf(AddressInUseException.class)
                .hasMessage("此地址已被訂單使用，無法刪除");

        verify(addressRepository).delete(usedAddress);
    }

    // Ownership 與會員存在性測試。
    @Test
    void getAddressRejectsAddressOwnedByAnotherMember() {
        when(addressRepository.findByAddressIdAndMemberMemberId(9, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddress(1, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Address not found");
    }

    @Test
    void createAddressRejectsMissingMember() {
        when(memberRepository.findByIdForAddressWrite(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.createAddress(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member not found");

        verify(addressRepository, never()).save(any(Address.class));
    }

    // 測試資料建立方法。
    private AddressRequest request(boolean isDefault) {
        return new AddressRequest(
                request.receiverName(),
                request.receiverPhone(),
                request.postalCode(),
                request.city(),
                request.district(),
                request.detailAddress(),
                isDefault);
    }

    private void mockAddressWriteMember() {
        when(memberRepository.findByIdForAddressWrite(1)).thenReturn(Optional.of(member));
    }

    private Address address(Integer addressId, boolean isDefault) {
        Address address = new Address();
        address.setAddressId(addressId);
        address.setMember(member);
        address.setReceiverName("王小明");
        address.setReceiverPhone("0912345678");
        address.setPostalCode("100");
        address.setCity("台北市");
        address.setDistrict("中正區");
        address.setDetailAddress("忠孝西路一段 1 號");
        address.setIsDefault(isDefault);
        return address;
    }
}
