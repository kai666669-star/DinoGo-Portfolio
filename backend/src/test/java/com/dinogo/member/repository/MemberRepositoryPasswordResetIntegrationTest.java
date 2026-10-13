package com.dinogo.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.entity.Member;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-for-jwt-context-only-32-bytes",
        "app.password-reset.secret=test-password-reset-secret-must-be-at-least-32-bytes"
})
@Transactional
class MemberRepositoryPasswordResetIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void resetPasswordUpdateSucceedsOnceAndRejectsTheSameAuthVersionAfterward() {
        Member member = persistActiveMember();
        entityManager.flush();
        entityManager.clear();

        int firstUpdate = memberRepository.resetPasswordIfTokenIsValid(
                member.getMemberId(), member.getEmail(), 0, "first-password-hash");
        int repeatedUpdate = memberRepository.resetPasswordIfTokenIsValid(
                member.getMemberId(), member.getEmail(), 0, "second-password-hash");

        Member updatedMember = memberRepository.findById(member.getMemberId()).orElseThrow();
        assertThat(firstUpdate).isEqualTo(1);
        assertThat(repeatedUpdate).isZero();
        assertThat(updatedMember.getPasswordHash()).isEqualTo("first-password-hash");
        assertThat(updatedMember.getAuthVersion()).isEqualTo(1);
    }

    private Member persistActiveMember() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Member member = new Member();
        member.setEmail("password-reset-" + unique + "@example.com");
        member.setPasswordHash("original-password-hash");
        member.setLastName("Test");
        member.setFirstName("Member");
        member.setStatus("ACTIVE");
        entityManager.persist(member);
        return member;
    }
}
