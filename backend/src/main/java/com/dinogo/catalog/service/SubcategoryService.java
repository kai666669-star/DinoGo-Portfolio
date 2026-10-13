package com.dinogo.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.SubcategoryResponse;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.catalog.repository.SubcategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;

    // 查詢全部子分類
    public List<SubcategoryResponse> getAllSubcategories() {

        return subcategoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 根據 categoryId 查詢子分類
    public List<SubcategoryResponse> getSubcategoriesByCategoryId(Integer categoryId) {

        return subcategoryRepository.findByCategoryCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SubcategoryResponse toResponse(Subcategory subcategory) {

        return new SubcategoryResponse(
                subcategory.getSubcategoryId(),
                subcategory.getCategory().getCategoryId(),
                subcategory.getSubcategoryName());
    }
}