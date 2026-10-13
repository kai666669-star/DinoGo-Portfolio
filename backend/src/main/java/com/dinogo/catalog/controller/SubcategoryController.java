package com.dinogo.catalog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.catalog.dto.SubcategoryResponse;
import com.dinogo.catalog.service.SubcategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @GetMapping
    public List<SubcategoryResponse> getSubcategories(
            @RequestParam(required = false) Integer categoryId) {

        if (categoryId != null) {
            return subcategoryService.getSubcategoriesByCategoryId(categoryId);
        }

        return subcategoryService.getAllSubcategories();
    }
}