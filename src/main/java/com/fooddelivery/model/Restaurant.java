package com.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cuisine_type")
    private String cuisineType;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String phone;

    @Column(name = "image_url")
    private String imageUrl;

    private Double rating = 0.0;

    @Column(name = "delivery_time_minutes")
    private Integer deliveryTimeMinutes = 30;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Live open/closed toggle — controlled by the restaurant owner.
    // Distinct from isActive (which is the admin's enable/disable switch).
    @Column(name = "is_open")
    private Boolean isOpen = true;

    // The restaurant-owner account that manages this restaurant (nullable —
    // restaurants created directly by an admin may have no owner assigned yet).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"orders", "password", "hibernateLazyInitializer", "handler"})
    private User owner;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore   // Prevent cascade serialization of all menu items with restaurant
    private List<MenuItem> menuItems;
}
