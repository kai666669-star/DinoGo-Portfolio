package com.dinogo.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dinogo.catalog.dto.BrandResponse;
import com.dinogo.catalog.repository.BrandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandResponse> getAllBrands() {

        return brandRepository.findAll()
                .stream()
                .map(brand -> new BrandResponse(
                        brand.getBrandId(),
                        brand.getBrandName()))
                .toList();
    }
}