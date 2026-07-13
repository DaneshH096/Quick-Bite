package com.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore   // Never serialize password in any JSON response
    private String password;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore   // Never include full order list when serializing a User
    private List<Order> orders;

    public enum Role {
        CUSTOMER, ADMIN, DELIVERY_PARTNER, RESTAURANT_OWNER
    }
}
