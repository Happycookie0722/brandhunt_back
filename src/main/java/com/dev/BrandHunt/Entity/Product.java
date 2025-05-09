package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @NotNull
    private String name;

    @NotNull
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
}
