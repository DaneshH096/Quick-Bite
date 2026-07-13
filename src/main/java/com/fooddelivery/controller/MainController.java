package com.fooddelivery.controller;

import com.fooddelivery.model.User;
import com.fooddelivery.service.DeliveryPartnerService;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MainController {

    @Autowired private RestaurantService      restaurantService;
    @Autowired private UserService            userService;
    @Autowired private DeliveryPartnerService deliveryPartnerService;

    @GetMapping("/")
    public String root(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/home";
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/home";
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/home";
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, defaultValue = "CUSTOMER") String role,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String restaurantName,
            @RequestParam(required = false) String cuisineType,
            Model model) {
        try {
            if ("DELIVERY_PARTNER".equals(role)) {
                // Register as delivery partner via DeliveryPartnerService
                deliveryPartnerService.registerPartner(name, email, password, phone,
                    vehicleType != null ? vehicleType : "Bike");
            } else if ("RESTAURANT_OWNER".equals(role)) {
                // Register the owner account AND create their restaurant in one step
                if (restaurantName == null || restaurantName.isBlank()) {
                    throw new RuntimeException("Restaurant name is required");
                }
                restaurantService.registerRestaurantWithOwner(
                    name, email, password, phone, restaurantName, cuisineType, address);
            } else {
                // Normal customer registration
                userService.register(name, email, password, phone, address);
            }
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/home")
    public String homePage(Authentication auth,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String sort,
                           Model model) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        if (user.getRole() == User.Role.DELIVERY_PARTNER) return "redirect:/delivery";
        if (user.getRole() == User.Role.RESTAURANT_OWNER) return "redirect:/restaurant-portal";
        model.addAttribute("user", user);
        model.addAttribute("sort", sort);

        if (search != null && !search.isBlank()) {
            model.addAttribute("restaurants", restaurantService.searchRestaurantsSorted(search, sort));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("restaurants", restaurantService.getAllActiveRestaurantsSorted(sort));
        }
        return "home";
    }

    @GetMapping("/restaurant/{id}")
    public String restaurantPage(@PathVariable Long id, Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("restaurant",
            restaurantService.getRestaurantById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found")));
        model.addAttribute("menuItems", restaurantService.getMenuByRestaurant(id));
        return "restaurant";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        return "checkout";
    }

    @GetMapping("/orders")
    public String ordersPage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        return "orders";
    }

    @GetMapping("/profile")
    public String profilePage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByEmail(auth.getName()).orElseThrow());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
                                 @RequestParam String name,
                                 @RequestParam String phone,
                                 @RequestParam String address) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        userService.updateProfile(user.getId(), name, phone, address);
        return "redirect:/profile?updated=true";
    }
}
