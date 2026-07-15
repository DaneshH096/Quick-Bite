package com.fooddelivery.controller;

import com.fooddelivery.model.*;
import com.fooddelivery.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

/**
 * Restaurant-owner portal. Restaurant owners sign in through the same
 * /login page as everyone else (SecurityConfig routes ROLE_RESTAURANT_OWNER
 * here after login) and manage their own restaurant: profile + photo,
 * open/close status, menu/dish photos, and the incoming-order pipeline
 * (confirm → preparing → prepared), which then becomes visible to
 * delivery partners.
 */
@Controller
@RequestMapping("/restaurant-portal")
public class RestaurantController {

    @Autowired private RestaurantService restaurantService;
    @Autowired private OrderService      orderService;
    @Autowired private UserService       userService;

    // ── Resolve the restaurant owned by the signed-in user ──────────
    private Restaurant myRestaurant(Authentication auth) {
        User owner = userService.findByEmail(auth.getName()).orElseThrow();
        return restaurantService.getRestaurantByOwnerId(owner.getId())
            .orElseThrow(() -> new RuntimeException("No restaurant is linked to this account yet"));
    }

    // ── Dashboard ────────────────────────────────────────────────────
    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        User owner = userService.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("user", owner);
        Restaurant restaurant = myRestaurant(auth);
        model.addAttribute("restaurant", restaurant);

        List<Order> orders = orderService.getOrdersByRestaurant(restaurant.getId());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("pendingCount", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        model.addAttribute("activeCount", orders.stream().filter(o ->
            o.getStatus() == Order.OrderStatus.CONFIRMED || o.getStatus() == Order.OrderStatus.PREPARING).count());
        model.addAttribute("menuCount", restaurantService.getMenuItemsByRestaurant(restaurant.getId()).size());
        model.addAttribute("recentOrders", orders.stream().limit(8).toList());
        return "restaurant-portal/dashboard";
    }

    // ── Open / Close toggle (AJAX) ───────────────────────────────────
    @PostMapping("/toggle-open")
    @ResponseBody
    public ResponseEntity<?> toggleOpen(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Restaurant restaurant = myRestaurant(auth);
            boolean open = Boolean.parseBoolean(String.valueOf(body.get("open")));
            Restaurant updated = restaurantService.setOpenStatus(restaurant.getId(), open);
            return ResponseEntity.ok(Map.of("success", true, "isOpen", updated.getIsOpen()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Restaurant profile + photo ───────────────────────────────────
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("restaurant", myRestaurant(auth));
        return "restaurant-portal/profile";
    }

    @PostMapping("/profile/save")
    public String saveProfile(Authentication auth,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String cuisineType,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) Integer deliveryTimeMinutes,
                               @RequestParam(required = false) BigDecimal minOrderAmount,
                               @RequestParam(required = false) String imageUrl,
                               @RequestParam(required = false) MultipartFile photo) {
        Restaurant restaurant = myRestaurant(auth);
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setCuisineType(cuisineType);
        restaurant.setAddress(address);
        restaurant.setPhone(phone);
        if (deliveryTimeMinutes != null) restaurant.setDeliveryTimeMinutes(deliveryTimeMinutes);
        if (minOrderAmount != null)      restaurant.setMinOrderAmount(minOrderAmount);
        if (imageUrl != null && !imageUrl.isBlank()) restaurant.setImageUrl(imageUrl);
        restaurantService.saveRestaurant(restaurant);
        if (photo != null && !photo.isEmpty()) {
            restaurantService.saveRestaurantPhoto(restaurant.getId(), photo);
        }
        return "redirect:/restaurant-portal/profile?updated=true";
    }

    // ── Menu / dish management ───────────────────────────────────────
    @GetMapping("/menu")
    public String menu(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        Restaurant restaurant = myRestaurant(auth);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", restaurantService.getMenuItemsByRestaurant(restaurant.getId()));
        return "restaurant-portal/menu";
    }

    @PostMapping("/menu/save")
    public String saveMenuItem(Authentication auth,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile photo,
                                @RequestParam(defaultValue = "false") Boolean isVegetarian,
                                @RequestParam(required = false) Long id) {
        Restaurant restaurant = myRestaurant(auth);

        MenuItem item;
        boolean isNew = (id == null);
        if (id != null) {
            item = restaurantService.getMenuItemById(id).orElse(new MenuItem());
            // Ownership check — never let one owner edit another restaurant's dish
            if (item.getRestaurant() != null && !item.getRestaurant().getId().equals(restaurant.getId())) {
                throw new RuntimeException("Not your menu item");
            }
        } else {
            item = new MenuItem();
        }
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setCategory(category);
        if (imageUrl != null && !imageUrl.isBlank()) item.setImageUrl(imageUrl);
        item.setIsVegetarian(isVegetarian);
        // Only force "available" on brand-new dishes. Editing an existing dish must
        // NOT silently flip it back to available if the owner had 86'd it earlier —
        // and defend against a stray null ever being treated as unavailable.
        if (isNew || item.getIsAvailable() == null) {
            item.setIsAvailable(true);
        }
        restaurantService.saveMenuItemWithPhoto(item, photo);
        return "redirect:/restaurant-portal/menu";
    }

    @GetMapping("/menu/delete/{itemId}")
    public String deleteMenuItem(@PathVariable Long itemId, Authentication auth) {
        Restaurant restaurant = myRestaurant(auth);
        restaurantService.getMenuItemById(itemId).ifPresent(item -> {
            if (item.getRestaurant() != null && item.getRestaurant().getId().equals(restaurant.getId())) {
                restaurantService.deleteMenuItem(itemId);
            }
        });
        return "redirect:/restaurant-portal/menu";
    }

    // Toggle a single dish's availability (in stock / 86'd) without a full edit
    @PostMapping("/menu/{itemId}/availability")
    @ResponseBody
    public ResponseEntity<?> toggleItemAvailability(@PathVariable Long itemId,
                                                     @RequestBody Map<String, Object> body,
                                                     Authentication auth) {
        try {
            Restaurant restaurant = myRestaurant(auth);
            MenuItem item = restaurantService.getMenuItemById(itemId).orElseThrow();
            if (item.getRestaurant() == null || !item.getRestaurant().getId().equals(restaurant.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not your menu item"));
            }
            item.setIsAvailable(Boolean.parseBoolean(String.valueOf(body.get("available"))));
            restaurantService.saveMenuItem(item);
            return ResponseEntity.ok(Map.of("success", true, "isAvailable", item.getIsAvailable()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Incoming orders + status pipeline ────────────────────────────
    @GetMapping("/orders")
    public String orders(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        Restaurant restaurant = myRestaurant(auth);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("orders", orderService.getOrdersByRestaurant(restaurant.getId()));
        return "restaurant-portal/orders";
    }

    private void assertOwnsOrder(Restaurant restaurant, Order order) {
        if (order.getRestaurant() == null || !order.getRestaurant().getId().equals(restaurant.getId())) {
            throw new RuntimeException("This order does not belong to your restaurant");
        }
    }

    @PostMapping("/orders/{id}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirm(@PathVariable Long id, Authentication auth) {
        try {
            Restaurant restaurant = myRestaurant(auth);
            assertOwnsOrder(restaurant, orderService.getOrderById(id).orElseThrow());
            Order updated = orderService.confirmOrder(id);
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/preparing")
    @ResponseBody
    public ResponseEntity<?> preparing(@PathVariable Long id, Authentication auth) {
        try {
            Restaurant restaurant = myRestaurant(auth);
            assertOwnsOrder(restaurant, orderService.getOrderById(id).orElseThrow());
            Order updated = orderService.startPreparing(id);
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/prepared")
    @ResponseBody
    public ResponseEntity<?> prepared(@PathVariable Long id, Authentication auth) {
        try {
            Restaurant restaurant = myRestaurant(auth);
            assertOwnsOrder(restaurant, orderService.getOrderById(id).orElseThrow());
            Order updated = orderService.markPrepared(id);
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // NOTE: There used to be an "auto-assign nearest partner" action here that
    // force-picked whichever delivery partner happened to be online — which
    // meant a single online partner got every order with no say in it. That's
    // been removed. Once an order is marked PREPARED, it automatically shows
    // up in every online delivery partner's "Ready for Pickup" queue
    // (see DeliveryController#readyOrders), and assignment only happens when
    // a partner explicitly taps Accept (DeliveryController#acceptOrder).
}
