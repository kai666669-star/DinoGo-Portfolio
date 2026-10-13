package com.dinogo.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.MemberOAuthAccount;

public interface MemberOAuthAccountRepository extends JpaRepository<MemberOAuthAccount, Integer> {

    Optional<MemberOAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
