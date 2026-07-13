package com.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Prevent Jackson from following LAZY proxies on bidirectional links
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)   // ← EAGER: always load with order
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"orders", "password", "hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)   // ← EAGER: always load with order
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"menuItems", "hibernateLazyInitializer", "handler"})
    private Restaurant restaurant;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "delivery_address", columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "delivery_partner_id")
    private Long deliveryPartnerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"order"})
    private List<OrderItem> orderItems;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Full lifecycle, in order:
    //   PENDING       — customer just placed the order
    //   CONFIRMED     — restaurant accepted it
    //   PREPARING     — restaurant is cooking
    //   PREPARED      — food ready; becomes visible/assignable to delivery partners
    //   PICKED_UP     — delivery partner collected the food from the restaurant
    //   OUT_FOR_DELIVERY — partner is on the way to the customer
    //   DELIVERED     — completed
    //   CANCELLED     — terminal, can happen from most non-delivered states
    public enum OrderStatus {
        PENDING, CONFIRMED, PREPARING, PREPARED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    }

    public enum PaymentMethod {
        CASH, ONLINE
    }
}
