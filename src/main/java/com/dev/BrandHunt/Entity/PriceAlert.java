package com.dev.BrandHunt.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "price_alerts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_price_alert_user_product",
                columnNames = {"user_id", "product_id"}
        ))
public class PriceAlert extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    public PriceAlert(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
