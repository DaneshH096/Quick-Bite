package com.fooddelivery.service;

import com.fooddelivery.model.*;
import com.fooddelivery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    @Autowired private OrderRepository      orderRepository;
    @Autowired private MenuItemRepository   menuItemRepository;
    @Autowired private UserRepository       userRepository;
    @Autowired private RestaurantRepository restaurantRepository;

    // ── Place order ────────────────────────────────────────────────
    @Transactional
    public Order placeOrder(Long userId, Long restaurantId,
                             Map<Long, Integer> cartItems,
                             String deliveryAddress,
                             String paymentMethod,
                             String specialInstructions,
                             Double deliveryLat,
                             Double deliveryLng) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (!Boolean.TRUE.equals(restaurant.getIsActive())) {
            throw new RuntimeException("This restaurant is not accepting orders right now");
        }
        if (!Boolean.TRUE.equals(restaurant.getIsOpen())) {
            throw new RuntimeException(restaurant.getName() + " is currently closed");
        }

        Order order = Order.builder()
            .user(user)
            .restaurant(restaurant)
            .deliveryAddress(deliveryAddress)
            .paymentMethod(Order.PaymentMethod.valueOf(paymentMethod))
            .specialInstructions(specialInstructions)
            .status(Order.OrderStatus.PENDING)
            .totalAmount(BigDecimal.ZERO)
            .deliveryLat(deliveryLat)
            .deliveryLng(deliveryLng)
            .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            MenuItem item = menuItemRepository.findById(entry.getKey())
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + entry.getKey()));
            int qty = entry.getValue();
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            orderItems.add(OrderItem.builder()
                .order(savedOrder)
                .menuItem(item)
                .quantity(qty)
                .unitPrice(item.getPrice())
                .subtotal(subtotal)
                .build());
        }

        savedOrder.setOrderItems(orderItems);
        savedOrder.setTotalAmount(total);
        return orderRepository.save(savedOrder);
    }

    // ── Assign delivery partner ────────────────────────────────────
    @Transactional
    public Order assignDeliveryPartner(Long orderId, Long partnerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        User partner = userRepository.findById(partnerId)
            .orElseThrow(() -> new RuntimeException("Delivery partner not found"));

        if (partner.getRole() != User.Role.DELIVERY_PARTNER)
            throw new RuntimeException("User is not a delivery partner");

        // Guard against two partners both tapping "Accept" on the same ready
        // order at nearly the same time — first one in wins, the second gets
        // a clear error instead of silently overwriting the first partner.
        if (order.getDeliveryPartnerId() != null && !order.getDeliveryPartnerId().equals(partnerId)) {
            throw new RuntimeException("This order was just accepted by another delivery partner");
        }

        // Assigning a partner does NOT change the order status by itself —
        // the food may still be PREPARING. The partner physically collects it
        // and calls markPickedUp() themselves once the restaurant hands it over.
        order.setDeliveryPartnerId(partnerId);
        partner.setIsAvailable(false);
        userRepository.save(partner);
        return orderRepository.save(order);
    }

    // ── Auto-assign nearest ────────────────────────────────────────
    // NOTE: A previous version of this method ("autoAssignNearestPartner")
    // force-assigned an order to whichever delivery partner happened to be
    // nearest/online — with no acceptance step. That meant a single online
    // partner got every order automatically, with zero say in it. Assignment
    // now only ever happens two ways:
    //   1. An admin explicitly picks a specific partner (assignDeliveryPartner
    //      above, called from AdminController).
    //   2. A delivery partner sees the order in their own "Ready for Pickup"
    //      queue (getReadyForPickupOrders below) and explicitly accepts it
    //      themselves (also routes through assignDeliveryPartner).

    // ── Update order status ────────────────────────────────────────
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        order.setStatus(newStatus);

        if ((newStatus == Order.OrderStatus.DELIVERED ||
             newStatus == Order.OrderStatus.CANCELLED)
            && order.getDeliveryPartnerId() != null) {
            userRepository.findById(order.getDeliveryPartnerId()).ifPresent(p -> {
                p.setIsAvailable(true);
                userRepository.save(p);
            });
        }
        return orderRepository.save(order);
    }

    // ── Restaurant-side pipeline: PENDING → CONFIRMED → PREPARING → PREPARED ─
    // Kept as explicit, named steps (rather than one generic setter) so the
    // restaurant portal can only ever push an order forward one valid stage
    // at a time — never skip ahead or jump backwards by accident.
    @Transactional
    public Order confirmOrder(Long orderId) {
        return advanceStatus(orderId, Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED);
    }

    @Transactional
    public Order startPreparing(Long orderId) {
        return advanceStatus(orderId, Order.OrderStatus.CONFIRMED, Order.OrderStatus.PREPARING);
    }

    @Transactional
    public Order markPrepared(Long orderId) {
        // Food is ready — this is the point the order becomes visible/assignable
        // to delivery partners (see getReadyForPickupOrders below).
        return advanceStatus(orderId, Order.OrderStatus.PREPARING, Order.OrderStatus.PREPARED);
    }

    // ── Delivery-partner-side pipeline: PREPARED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED ─
    @Transactional
    public Order markPickedUp(Long orderId, Long partnerId) {
        Order order = requireOwnedByPartner(orderId, partnerId);
        requireStatus(order, Order.OrderStatus.PREPARED);
        order.setStatus(Order.OrderStatus.PICKED_UP);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markOutForDelivery(Long orderId, Long partnerId) {
        Order order = requireOwnedByPartner(orderId, partnerId);
        requireStatus(order, Order.OrderStatus.PICKED_UP);
        order.setStatus(Order.OrderStatus.OUT_FOR_DELIVERY);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markDeliveredByPartner(Long orderId, Long partnerId) {
        Order order = requireOwnedByPartner(orderId, partnerId);
        requireStatus(order, Order.OrderStatus.OUT_FOR_DELIVERY);
        order.setStatus(Order.OrderStatus.DELIVERED);
        userRepository.findById(partnerId).ifPresent(p -> {
            p.setIsAvailable(true);
            userRepository.save(p);
        });
        return orderRepository.save(order);
    }

    private Order advanceStatus(Long orderId, Order.OrderStatus expectedCurrent, Order.OrderStatus next) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        requireStatus(order, expectedCurrent);
        order.setStatus(next);
        return orderRepository.save(order);
    }

    private Order requireOwnedByPartner(Long orderId, Long partnerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!partnerId.equals(order.getDeliveryPartnerId())) {
            throw new RuntimeException("This order is not assigned to you");
        }
        return order;
    }

    private void requireStatus(Order order, Order.OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new RuntimeException(
                "Order #" + order.getId() + " is " + order.getStatus() +
                ", expected " + expected + " for this action");
        }
    }

    // ── Read methods — @Transactional(readOnly) keeps session open ─
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    // Orders that are PREPARED (ready) but have no delivery partner yet —
    // the pool delivery partners/admin can assign from.
    @Transactional(readOnly = true)
    public List<Order> getReadyForPickupOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(Order.OrderStatus.PREPARED);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByDeliveryPartner(Long partnerId) {
        return orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getActiveOrderForPartner(Long partnerId) {
        // Any order currently assigned to this partner that isn't finished yet —
        // covers "awaiting pickup" (PREPARED), "collected" (PICKED_UP) and "en route".
        return orderRepository.findFirstByDeliveryPartnerIdAndStatusIn(partnerId, List.of(
            Order.OrderStatus.PREPARED,
            Order.OrderStatus.PICKED_UP,
            Order.OrderStatus.OUT_FOR_DELIVERY
        ));
    }
}
