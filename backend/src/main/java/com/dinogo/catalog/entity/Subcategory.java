package com.dinogo.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Subcategory", schema = "catalog", uniqueConstraints = {
                @UniqueConstraint(name = "uk_subcategory_category_name", columnNames = {
                                "category_id",
                                "subcategory_name"
                })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "subcategory_id")
        private Integer subcategoryId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "category_id", nullable = false)
        private Category category;

        @Column(name = "subcategory_name", nullable = false, length = 100)
        private String subcategoryName;
        @JsonIgnore
        @OneToMany(mappedBy = "subcategory")
        @Builder.Default
        private List<Product> products = new ArrayList<>();
}