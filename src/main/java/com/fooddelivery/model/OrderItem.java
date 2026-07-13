package com.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"orderItems", "user", "restaurant", "hibernateLazyInitializer", "handler"})
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_item_id", nullable = false)
    @JsonIgnoreProperties({"restaurant", "hibernateLazyInitializer", "handler"})
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;
}
