package com.dinogo.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
}