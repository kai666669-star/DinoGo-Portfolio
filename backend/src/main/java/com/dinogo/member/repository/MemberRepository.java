package com.dinogo.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.member.entity.Member;

import jakarta.persistence.LockModeType;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    public Optional<Member> findByEmail(String email);

    public Optional<Member> findByEmailIgnoreCase(String email);

    public boolean existsByEmail(String email);

    public boolean existsByEmailIgnoreCase(String email);

    /** Updates only self-service profile fields so security state cannot be overwritten. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.lastName = :lastName,
                   member.firstName = :firstName,
                   member.birthDate = :birthDate,
                   member.phone = :phone,
                   member.emailOrderNotifications = coalesce(:emailOrderNotifications, member.emailOrderNotifications),
                   member.emailMarketingNotifications = coalesce(:emailMarketingNotifications, member.emailMarketingNotifications),
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
            """)
    int updateProfileFields(
            @Param("memberId") Integer memberId,
            @Param("lastName") String lastName,
            @Param("firstName") String firstName,
            @Param("birthDate") java.time.LocalDate birthDate,
            @Param("phone") String phone,
            @Param("emailOrderNotifications") Boolean emailOrderNotifications,
            @Param("emailMarketingNotifications") Boolean emailMarketingNotifications);

    /** Invalidates existing JWTs without writing unrelated Member columns. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.authVersion = member.authVersion + 1,
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
            """)
    int increaseAuthVersion(@Param("memberId") Integer memberId);

    /**
     * Changes a password without flushing a previously loaded Member entity back to the database.
     * The current hash and auth version make concurrent password or account-state changes fail safely.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.passwordHash = :newPasswordHash,
                   member.authVersion = member.authVersion + 1,
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
               and member.passwordHash = :currentPasswordHash
               and member.authVersion = :authVersion
               and member.status = 'ACTIVE'
            """)
    int changePasswordIfCurrent(
            @Param("memberId") Integer memberId,
            @Param("currentPasswordHash") String currentPasswordHash,
            @Param("authVersion") int authVersion,
            @Param("newPasswordHash") String newPasswordHash);

    /** Updates account state only when it still matches the state that was validated. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.status = :nextStatus,
                   member.authVersion = member.authVersion + 1,
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
               and member.status = :expectedStatus
            """)
    int updateStatusIfCurrent(
            @Param("memberId") Integer memberId,
            @Param("expectedStatus") String expectedStatus,
            @Param("nextStatus") String nextStatus);

    /** Serializes address writes for one member, including the first-address decision. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from Member member where member.memberId = :memberId")
    Optional<Member> findByIdForAddressWrite(@Param("memberId") Integer memberId);

    /**
     * Atomically updates a password only while the reset token's member state is still current.
     * A return value of zero means the token was already consumed or is no longer valid.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member member
               set member.passwordHash = :passwordHash,
                   member.authVersion = member.authVersion + 1,
                   member.updatedAt = CURRENT_TIMESTAMP
             where member.memberId = :memberId
               and member.authVersion = :authVersion
               and member.status = 'ACTIVE'
               and lower(member.email) = lower(:email)
            """)
    int resetPasswordIfTokenIsValid(
            @Param("memberId") Integer memberId,
            @Param("email") String email,
            @Param("authVersion") int authVersion,
            @Param("passwordHash") String passwordHash);

    @EntityGraph(attributePaths = { "memberRoles", "memberRoles.role" })
    public List<Member> findAllByStatusIgnoreCase(String status);

    @EntityGraph(attributePaths = { "memberRoles", "memberRoles.role" })
    List<Member> findAllByOrderByCreatedAtDesc();
}
