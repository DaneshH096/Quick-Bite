package com.fooddelivery.controller;

import com.fooddelivery.model.*;
import com.fooddelivery.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired private OrderService           orderService;
    @Autowired private RestaurantService      restaurantService;
    @Autowired private UserService            userService;
    @Autowired private DeliveryPartnerService deliveryPartnerService;

    // ── Restaurants ────────────────────────────────────────────────
    @GetMapping("/restaurants")
    public ResponseEntity<List<Restaurant>> getRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants());
    }

    @GetMapping("/restaurants/{id}")
    public ResponseEntity<?> getRestaurant(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id)
            .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getMenuByRestaurant(id));
    }

    // ── POST place order ───────────────────────────────────────────
    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, Object> payload,
                                         Authentication auth) {
        try {
            User user = userService.findByEmail(auth.getName()).orElseThrow();
            Long restaurantId   = Long.valueOf(payload.get("restaurantId").toString());
            String address      = payload.get("deliveryAddress").toString();
            String payment      = payload.getOrDefault("paymentMethod", "CASH").toString();
            String instructions = payload.getOrDefault("specialInstructions", "").toString();

            Double lat = payload.containsKey("deliveryLat") && payload.get("deliveryLat") != null
                ? Double.parseDouble(payload.get("deliveryLat").toString()) : null;
            Double lng = payload.containsKey("deliveryLng") && payload.get("deliveryLng") != null
                ? Double.parseDouble(payload.get("deliveryLng").toString()) : null;

            @SuppressWarnings("unchecked")
            Map<String, Object> rawCart = (Map<String, Object>) payload.get("cartItems");
            Map<Long, Integer> cartItems = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rawCart.entrySet())
                cartItems.put(Long.valueOf(e.getKey()), Integer.valueOf(e.getValue().toString()));

            Order order = orderService.placeOrder(
                user.getId(), restaurantId, cartItems,
                address, payment, instructions, lat, lng);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success",     true);
            resp.put("orderId",     order.getId());
            resp.put("totalAmount", order.getTotalAmount());
            resp.put("status",      order.getStatus());
            resp.put("hasGps",      lat != null && lng != null);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── GET /api/orders/my — returns safe DTOs (no lazy proxy issues) ──
    @GetMapping("/orders/my")
    public ResponseEntity<List<Map<String, Object>>> getMyOrders(Authentication auth) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        List<Order> orders = orderService.getOrdersByUser(user.getId());
        return ResponseEntity.ok(orders.stream().map(this::toOrderDto).toList());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderById(id)
            .map(o -> ResponseEntity.ok(toOrderDto(o)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Delivery partners (already safe — no entity returned) ──────
    @GetMapping("/delivery-partners/nearest")
    public ResponseEntity<?> nearestPartners(
            @RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(
            deliveryPartnerService.getNearestPartners(lat, lng)
                .stream().map(this::toPartnerDto).toList());
    }

    @GetMapping("/delivery-partners")
    public ResponseEntity<?> allPartners() {
        return ResponseEntity.ok(
            deliveryPartnerService.getAllPartners()
                .stream().map(this::toPartnerDto).toList());
    }

    // ══════════════════════════════════════════════════════════════
    // DTO helpers — convert JPA entities → plain Maps so Jackson
    // never touches a LAZY proxy outside a transaction session.
    // ══════════════════════════════════════════════════════════════

    private Map<String, Object> toOrderDto(Order o) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",              o.getId());
        dto.put("totalAmount",     o.getTotalAmount());
        dto.put("deliveryAddress", o.getDeliveryAddress());
        dto.put("status",          o.getStatus() != null ? o.getStatus().name() : "PENDING");
        dto.put("paymentMethod",   o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "CASH");
        dto.put("specialInstructions", o.getSpecialInstructions());
        dto.put("deliveryLat",     o.getDeliveryLat());
        dto.put("deliveryLng",     o.getDeliveryLng());
        dto.put("deliveryPartnerId", o.getDeliveryPartnerId());
        dto.put("createdAt",       o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        dto.put("updatedAt",       o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);

        // Restaurant — eagerly safe via getter (already loaded or simple read)
        if (o.getRestaurant() != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id",          o.getRestaurant().getId());
            r.put("name",        o.getRestaurant().getName());
            r.put("cuisineType", o.getRestaurant().getCuisineType());
            r.put("imageUrl",    o.getRestaurant().getImageUrl());
            dto.put("restaurant", r);
        } else {
            dto.put("restaurant", null);
        }

        // Order items — EAGER so always loaded
        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getOrderItems() != null) {
            for (OrderItem oi : o.getOrderItems()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",       oi.getId());
                item.put("quantity", oi.getQuantity());
                item.put("unitPrice",oi.getUnitPrice());
                item.put("subtotal", oi.getSubtotal());
                if (oi.getMenuItem() != null) {
                    Map<String, Object> mi = new LinkedHashMap<>();
                    mi.put("id",          oi.getMenuItem().getId());
                    mi.put("name",        oi.getMenuItem().getName());
                    mi.put("price",       oi.getMenuItem().getPrice());
                    mi.put("category",    oi.getMenuItem().getCategory());
                    mi.put("isVegetarian",oi.getMenuItem().getIsVegetarian());
                    item.put("menuItem", mi);
                }
                items.add(item);
            }
        }
        dto.put("orderItems", items);
        return dto;
    }

    private Map<String, Object> toPartnerDto(User p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          p.getId());
        m.put("name",        p.getName());
        m.put("phone",       p.getPhone());
        m.put("vehicleType", p.getVehicleType());
        m.put("currentLat",  p.getCurrentLat());
        m.put("currentLng",  p.getCurrentLng());
        m.put("isAvailable", p.getIsAvailable());
        return m;
    }
}
