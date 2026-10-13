package com.dinogo.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.seller.entity.SellerApplication;
import com.dinogo.seller.entity.SellerApplicationStatus;
import com.dinogo.seller.repository.SellerApplicationRepository;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class SellerApplicationServiceTest {

    @Mock
    private SellerApplicationRepository sellerApplicationRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SellerService sellerService;

    @InjectMocks
    private SellerApplicationService sellerApplicationService;

    @Test
    void approveApplicationCreatesSellerAndRecordsReview() {
        Member member = new Member();
        member.setMemberId(7);

        SellerApplication application = new SellerApplication();
        application.setApplicationId(11);
        application.setMember(member);
        application.setStoreName("森野選物所");
        application.setStoreDescription("自然選物");
        application.setStoreLogoUrl("https://res.cloudinary.com/demo/image/upload/logo.png");
        application.setStatus(SellerApplicationStatus.PENDING);

        when(sellerApplicationRepository.findById(11)).thenReturn(Optional.of(application));
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerApplicationService service = sellerApplicationService;
        var result = service.approveApplication(11, 99);

        assertThat(result.status()).isEqualTo(SellerApplicationStatus.APPROVED);
        assertThat(result.reviewedBy()).isEqualTo(99);
        assertThat(result.reviewedAt()).isNotNull();
        verify(sellerService).createSellerFromApplication(
                7,
                "森野選物所",
                "自然選物",
                "https://res.cloudinary.com/demo/image/upload/logo.png");
        verify(sellerApplicationRepository).save(application);
    }
}
