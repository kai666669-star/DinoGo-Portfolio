package com.dinogo.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    public Optional<Role> findByRoleName(String roleName);
}
