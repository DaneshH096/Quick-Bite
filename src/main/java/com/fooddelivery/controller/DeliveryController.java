package com.fooddelivery.controller;

import com.fooddelivery.model.*;
import com.fooddelivery.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles the delivery partner portal: dashboard, location ping, order accept/complete.
 * Routes under /delivery/** require ROLE_DELIVERY_PARTNER.
 */
@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired private DeliveryPartnerService deliveryPartnerService;
    @Autowired private OrderService           orderService;
    @Autowired private UserService            userService;

    // ── Delivery partner dashboard ─────────────────────────────────
    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        User partner = userService.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("partner", partner);

        // Active order (OUT_FOR_DELIVERY assigned to me)
        orderService.getActiveOrderForPartner(partner.getId())
            .ifPresent(o -> model.addAttribute("activeOrder", o));

        // My completed deliveries today (last 20)
        List<Order> myOrders = orderService.getOrdersByDeliveryPartner(partner.getId());
        model.addAttribute("myOrders", myOrders.stream().limit(20).toList());
        return "delivery/dashboard";
    }

    // ── REST: push my GPS location ─────────────────────────────────
    @PostMapping("/location")
    @ResponseBody
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, Object> body,
                                             Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            double lat = Double.parseDouble(body.get("lat").toString());
            double lng = Double.parseDouble(body.get("lng").toString());
            deliveryPartnerService.updatePartnerLocation(partner.getId(), lat, lng);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── REST: toggle availability ──────────────────────────────────
    @PostMapping("/availability")
    @ResponseBody
    public ResponseEntity<?> toggleAvailability(@RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            boolean available = Boolean.parseBoolean(body.get("available").toString());
            User updated = deliveryPartnerService.setAvailability(partner.getId(), available);
            return ResponseEntity.ok(Map.of("success", true, "isAvailable", updated.getIsAvailable()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── REST: mark order as PICKED_UP (collected from restaurant) ──
    @PostMapping("/orders/{orderId}/picked-up")
    @ResponseBody
    public ResponseEntity<?> markPickedUp(@PathVariable Long orderId, Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            Order updated = orderService.markPickedUp(orderId, partner.getId());
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── REST: mark order OUT_FOR_DELIVERY (left the restaurant) ────
    @PostMapping("/orders/{orderId}/out-for-delivery")
    @ResponseBody
    public ResponseEntity<?> markOutForDelivery(@PathVariable Long orderId, Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            Order updated = orderService.markOutForDelivery(orderId, partner.getId());
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── REST: mark active order as DELIVERED ───────────────────────
    @PostMapping("/orders/{orderId}/delivered")
    @ResponseBody
    public ResponseEntity<?> markDelivered(@PathVariable Long orderId, Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            Order updated = orderService.markDeliveredByPartner(orderId, partner.getId());
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── REST: pool of PREPARED orders awaiting a delivery partner ──
    @GetMapping("/orders/ready")
    @ResponseBody
    public ResponseEntity<?> readyOrders() {
        List<Map<String, Object>> dtos = orderService.getReadyForPickupOrders().stream()
            .filter(o -> o.getDeliveryPartnerId() == null)
            .map(o -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", o.getId());
                m.put("restaurantName", o.getRestaurant() != null ? o.getRestaurant().getName() : null);
                m.put("deliveryAddress", o.getDeliveryAddress());
                m.put("totalAmount", o.getTotalAmount());
                return m;
            }).toList();
        return ResponseEntity.ok(dtos);
    }

    // ── REST: partner self-assigns a ready order ────────────────────
    @PostMapping("/orders/{orderId}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptOrder(@PathVariable Long orderId, Authentication auth) {
        try {
            User partner = userService.findByEmail(auth.getName()).orElseThrow();
            orderService.assignDeliveryPartner(orderId, partner.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
