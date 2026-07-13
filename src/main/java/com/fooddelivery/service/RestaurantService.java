package com.fooddelivery.service;

import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RestaurantService {

    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private MenuItemRepository   menuItemRepository;
    @Autowired private UserRepository       userRepository;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private FileStorageService   fileStorageService;

    // ── Restaurant-owner signup: creates the login AND the restaurant ──────
    @Transactional
    public Restaurant registerRestaurantWithOwner(String ownerName, String email, String password,
                                                   String phone, String restaurantName,
                                                   String cuisineType, String address) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }
        User owner = User.builder()
            .name(ownerName)
            .email(email)
            .password(passwordEncoder.encode(password))
            .phone(phone)
            .role(User.Role.RESTAURANT_OWNER)
            .isAvailable(true)
            .createdAt(LocalDateTime.now())
            .build();
        owner = userRepository.save(owner);

        Restaurant restaurant = Restaurant.builder()
            .name(restaurantName)
            .cuisineType(cuisineType)
            .address(address)
            .phone(phone)
            .owner(owner)
            .isActive(true)
            .isOpen(true)
            .rating(0.0)
            .deliveryTimeMinutes(30)
            .minOrderAmount(java.math.BigDecimal.ZERO)
            .createdAt(LocalDateTime.now())
            .build();
        return restaurantRepository.save(restaurant);
    }

    public Optional<Restaurant> getRestaurantByOwnerId(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId);
    }

    // ── Open / close toggle (restaurant owner's live status) ───────────────
    @Transactional
    public Restaurant setOpenStatus(Long restaurantId, boolean open) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        restaurant.setIsOpen(open);
        return restaurantRepository.save(restaurant);
    }

    // ── Photo-upload-aware saves — falls back to a manually typed URL ──────
    public Restaurant saveRestaurantPhoto(Long restaurantId, MultipartFile photo) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        String url = fileStorageService.store(photo, "restaurants");
        if (url != null) restaurant.setImageUrl(url);
        return restaurantRepository.save(restaurant);
    }

    public MenuItem saveMenuItemWithPhoto(MenuItem item, MultipartFile photo) {
        String url = fileStorageService.store(photo, "menu-items");
        if (url != null) item.setImageUrl(url);
        return menuItemRepository.save(item);
    }

    // ── All active restaurants, with optional sort ─────────────────
    public List<Restaurant> getAllActiveRestaurants() {
        return restaurantRepository.findByIsActiveTrue();
    }

    public List<Restaurant> getAllActiveRestaurantsSorted(String sort) {
        if (sort == null || sort.isBlank()) return getAllActiveRestaurants();
        return switch (sort) {
            case "rating"   -> restaurantRepository.findByIsActiveTrueOrderByRatingDesc();
            case "delivery" -> restaurantRepository.findByIsActiveTrueOrderByDeliveryTimeMinutesAsc();
            case "cost"     -> restaurantRepository.findByIsActiveTrueOrderByMinOrderAmountAsc();
            default         -> getAllActiveRestaurants();
        };
    }

    // ── Search with optional sort ──────────────────────────────────
    public List<Restaurant> searchRestaurants(String query) {
        return restaurantRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query);
    }

    public List<Restaurant> searchRestaurantsSorted(String query, String sort) {
        if (sort == null || sort.isBlank()) return searchRestaurants(query);
        return switch (sort) {
            case "rating"   -> restaurantRepository.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByRatingDesc(query);
            case "delivery" -> restaurantRepository.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByDeliveryTimeMinutesAsc(query);
            case "cost"     -> restaurantRepository.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByMinOrderAmountAsc(query);
            default         -> searchRestaurants(query);
        };
    }

    public Optional<Restaurant> getRestaurantById(Long id) {
        return restaurantRepository.findById(id);
    }

    public List<MenuItem> getMenuByRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId);
    }

    public Restaurant saveRestaurant(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    public void deleteRestaurant(Long id) {
        restaurantRepository.deleteById(id);
    }

    public MenuItem saveMenuItem(MenuItem item) {
        return menuItemRepository.save(item);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public List<MenuItem> getMenuItemsByRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }
}
