package com.fooddelivery.repository;

import com.fooddelivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // Restaurant owned by a given restaurant-owner user account
    Optional<Restaurant> findByOwnerId(Long ownerId);

    List<Restaurant> findByIsActiveTrue();

    // Search
    List<Restaurant> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    // Sort by rating DESC (highest first)
    List<Restaurant> findByIsActiveTrueOrderByRatingDesc();

    // Sort by delivery time ASC (fastest first)
    List<Restaurant> findByIsActiveTrueOrderByDeliveryTimeMinutesAsc();

    // Sort by min order amount ASC (cheapest first)
    List<Restaurant> findByIsActiveTrueOrderByMinOrderAmountAsc();

    // Search + sort
    List<Restaurant> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByRatingDesc(String name);
    List<Restaurant> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByDeliveryTimeMinutesAsc(String name);
    List<Restaurant> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByMinOrderAmountAsc(String name);
}
