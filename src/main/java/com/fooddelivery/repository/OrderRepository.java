package com.fooddelivery.repository;

import com.fooddelivery.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByDeliveryPartnerIdOrderByCreatedAtDesc(Long deliveryPartnerId);
    List<Order> findByDeliveryPartnerIdAndStatus(Long deliveryPartnerId, Order.OrderStatus status);
    Optional<Order> findFirstByDeliveryPartnerIdAndStatus(Long deliveryPartnerId, Order.OrderStatus status);
    List<Order> findByStatusAndDeliveryLatIsNotNull(Order.OrderStatus status);

    // Orders belonging to one restaurant (restaurant-owner portal)
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    // Orders currently sitting in a given status (e.g. PREPARED = ready for pickup pool)
    List<Order> findByStatusOrderByCreatedAtAsc(Order.OrderStatus status);

    // Active order (any of several in-flight statuses) assigned to a delivery partner
    Optional<Order> findFirstByDeliveryPartnerIdAndStatusIn(Long deliveryPartnerId, List<Order.OrderStatus> statuses);
}
