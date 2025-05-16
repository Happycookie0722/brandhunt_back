package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter @Setter @Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull
    private String name;

    @NotNull
    private int price;

    @ColumnDefault("0")
    private int salePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
}
