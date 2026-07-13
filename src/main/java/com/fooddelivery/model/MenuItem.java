package com.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"menuItems", "hibernateLazyInitializer", "handler"})
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_vegetarian")
    private Boolean isVegetarian = false;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
