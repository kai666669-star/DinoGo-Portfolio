package com.dinogo.member.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dinogo.member.entity.MemberAccountStatusHistory;

public interface MemberAccountStatusHistoryRepository extends JpaRepository<MemberAccountStatusHistory, Integer> {
    List<MemberAccountStatusHistory> findByMemberIdOrderByChangedAtDesc(Integer memberId);
}
