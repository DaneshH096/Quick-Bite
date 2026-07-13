package com.fooddelivery.controller;

import com.fooddelivery.model.*;
import com.fooddelivery.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private RestaurantService      restaurantService;
    @Autowired private OrderService           orderService;
    @Autowired private UserService            userService;
    @Autowired private DeliveryPartnerService deliveryPartnerService;

    // ── Dashboard ──────────────────────────────────────────────────
    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        List<Order>  orders      = orderService.getAllOrders();
        List<User>   partners    = deliveryPartnerService.getAllPartners();
        model.addAttribute("user", user);
        model.addAttribute("restaurants",       restaurantService.getAllRestaurants());
        model.addAttribute("orders", orders != null ? orders : new ArrayList<>());
        model.addAttribute("partners", partners != null ? partners : List.of());
        model.addAttribute("totalRestaurants",  restaurantService.getAllRestaurants().size());
        model.addAttribute("totalOrders",       orders.size());
        model.addAttribute("totalPartners",     partners.size());
        model.addAttribute("availablePartners", partners.stream().filter(p -> Boolean.TRUE.equals(p.getIsAvailable())).count());
        return "admin/dashboard";
    }

    // ── Restaurants ────────────────────────────────────────────────
    @GetMapping("/restaurants")
    public String listRestaurants(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("restaurants", restaurantService.getAllRestaurants());
        return "admin/restaurants";
    }

    @GetMapping("/restaurants/add")
    public String addRestaurantForm(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        return "admin/restaurant-form";
    }

    @PostMapping("/restaurants/save")
    public String saveRestaurant(@RequestParam String name,
                                  @RequestParam String description,
                                  @RequestParam String cuisineType,
                                  @RequestParam String address,
                                  @RequestParam String phone,
                                  @RequestParam(required = false) String imageUrl,
                                  @RequestParam Integer deliveryTimeMinutes,
                                  @RequestParam BigDecimal minOrderAmount,
                                  @RequestParam(required = false) Long id) {
        Restaurant r = id != null
            ? restaurantService.getRestaurantById(id).orElse(new Restaurant())
            : new Restaurant();
        r.setName(name); r.setDescription(description); r.setCuisineType(cuisineType);
        r.setAddress(address); r.setPhone(phone); r.setImageUrl(imageUrl);
        r.setDeliveryTimeMinutes(deliveryTimeMinutes); r.setMinOrderAmount(minOrderAmount);
        r.setIsActive(true);
        restaurantService.saveRestaurant(r);
        return "redirect:/admin/restaurants";
    }

    @GetMapping("/restaurants/edit/{id}")
    public String editRestaurant(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("restaurant", restaurantService.getRestaurantById(id).orElseThrow());
        return "admin/restaurant-form";
    }

    @GetMapping("/restaurants/delete/{id}")
    public String deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return "redirect:/admin/restaurants";
    }

    @GetMapping("/restaurants/{id}/menu")
    public String manageMenu(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("restaurant", restaurantService.getRestaurantById(id).orElseThrow());
        model.addAttribute("menuItems", restaurantService.getMenuItemsByRestaurant(id));
        return "admin/menu";
    }

    @PostMapping("/menu/save")
    public String saveMenuItem(@RequestParam Long restaurantId, @RequestParam String name,
                                @RequestParam String description, @RequestParam BigDecimal price,
                                @RequestParam String category,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(defaultValue = "false") Boolean isVegetarian,
                                @RequestParam(required = false) Long id) {
        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId).orElseThrow();
        MenuItem item = id != null
            ? restaurantService.getMenuItemById(id).orElse(new MenuItem()) : new MenuItem();
        item.setRestaurant(restaurant); item.setName(name); item.setDescription(description);
        item.setPrice(price); item.setCategory(category); item.setImageUrl(imageUrl);
        item.setIsVegetarian(isVegetarian); item.setIsAvailable(true);
        restaurantService.saveMenuItem(item);
        return "redirect:/admin/restaurants/" + restaurantId + "/menu";
    }

    @GetMapping("/menu/delete/{itemId}/{restaurantId}")
    public String deleteMenuItem(@PathVariable Long itemId, @PathVariable Long restaurantId) {
        restaurantService.deleteMenuItem(itemId);
        return "redirect:/admin/restaurants/" + restaurantId + "/menu";
    }

    // ── All Orders ─────────────────────────────────────────────────
    @GetMapping("/orders")
    public String allOrders(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("partners", deliveryPartnerService.getAllPartners());
        return "admin/orders";
    }

    // ── Update order status (AJAX) ─────────────────────────────────
    @PostMapping("/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        try {
            Order updated = orderService.updateOrderStatus(id, body.get("status"));
            return ResponseEntity.ok(Map.of("success", true, "status", updated.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Assign delivery partner (AJAX) ─────────────────────────────
    @PostMapping("/orders/{id}/assign")
    @ResponseBody
    public ResponseEntity<?> assignPartner(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        try {
            Long partnerId = Long.valueOf(body.get("partnerId").toString());
            Order updated  = orderService.assignDeliveryPartner(id, partnerId);
            User  partner  = deliveryPartnerService.findById(partnerId).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "success",     true,
                "status",      updated.getStatus(),
                "partnerName", partner.getName(),
                "partnerPhone",partner.getPhone()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Auto-assign nearest partner ────────────────────────────────
    @PostMapping("/orders/{id}/auto-assign")
    @ResponseBody
    public ResponseEntity<?> autoAssign(@PathVariable Long id) {
        try {
            Order updated = orderService.autoAssignNearestPartner(id);
            User partner  = deliveryPartnerService.findById(updated.getDeliveryPartnerId()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "success",     true,
                "partnerName", partner.getName(),
                "partnerPhone",partner.getPhone(),
                "status",      updated.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Delivery Partners management page ──────────────────────────
    @GetMapping("/delivery-partners")
    public String deliveryPartners(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("partners", deliveryPartnerService.getAllPartners());
        return "admin/delivery-partners";
    }

    // ── Add delivery partner ───────────────────────────────────────
    @PostMapping("/delivery-partners/add")
    public String addPartner(@RequestParam String name, @RequestParam String email,
                              @RequestParam String password, @RequestParam String phone,
                              @RequestParam String vehicleType, Model model) {
        try {
            deliveryPartnerService.registerPartner(name, email, password, phone, vehicleType);
            return "redirect:/admin/delivery-partners?added=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/delivery-partners?error=" + e.getMessage();
        }
    }

    // ── Live map page ──────────────────────────────────────────────
    @GetMapping("/map")
    public String liveMap(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        model.addAttribute("orders",   orderService.getAllOrders());
        model.addAttribute("partners", deliveryPartnerService.getAllPartners());
        return "admin/map";
    }
}
