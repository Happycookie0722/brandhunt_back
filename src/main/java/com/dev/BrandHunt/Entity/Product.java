package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "products",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_product_brand_external_id",
               columnNames = {"brand_id", "external_product_id"}
       ))
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull
    private String name;

    @NotNull
    private String img;

    @NotNull
    private String price;

    @ColumnDefault("0")
    private String salePrice;

    private String productUrl;

    @Column(name = "external_product_id")
    private String externalProductId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
}
