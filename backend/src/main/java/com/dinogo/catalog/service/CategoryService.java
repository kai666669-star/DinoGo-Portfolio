package com.dinogo.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.CategoryResponse;
import com.dinogo.catalog.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getCategoryId(),
                        category.getCategoryName()))
                .toList();
    }
}