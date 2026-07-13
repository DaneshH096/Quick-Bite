package com.fooddelivery.repository;

import com.fooddelivery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(User.Role role);

    // Find available delivery partners ordered by proximity (Haversine in SQL)
    @Query(value = """
        SELECT *, (
          6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(current_lat)) *
            COS(RADIANS(current_lng) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(current_lat))
          )
        ) AS distance_km
        FROM users
        WHERE role = 'DELIVERY_PARTNER'
          AND is_available = TRUE
          AND current_lat IS NOT NULL
          AND current_lng IS NOT NULL
        ORDER BY distance_km ASC
        LIMIT 10
        """, nativeQuery = true)
    List<User> findNearestAvailablePartners(double lat, double lng);
}
