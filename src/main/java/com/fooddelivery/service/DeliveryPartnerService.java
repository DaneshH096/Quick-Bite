package com.fooddelivery.service;

import com.fooddelivery.model.User;
import com.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryPartnerService {

    @Autowired private UserRepository  userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<User> getAllPartners() {
        return userRepository.findByRole(User.Role.DELIVERY_PARTNER);
    }

    @Transactional
    public User registerPartner(String name, String email, String password,
                                 String phone, String vehicleType) {
        if (userRepository.existsByEmail(email))
            throw new RuntimeException("Email already registered");

        /*
         * FIX: Lombok @Builder ignores field-level defaults.
         * Set createdAt explicitly so it's stored in DB.
         */
        return userRepository.save(User.builder()
            .name(name)
            .email(email)
            .password(passwordEncoder.encode(password))
            .phone(phone)
            .role(User.Role.DELIVERY_PARTNER)
            .vehicleType(vehicleType)
            .isAvailable(true)
            .createdAt(LocalDateTime.now())   // ← EXPLICIT
            .build());
    }

    @Transactional
    public User updatePartnerLocation(Long partnerId, double lat, double lng) {
        User partner = userRepository.findById(partnerId)
            .orElseThrow(() -> new RuntimeException("Partner not found"));
        partner.setCurrentLat(lat);
        partner.setCurrentLng(lng);
        return userRepository.save(partner);
    }

    @Transactional
    public User setAvailability(Long partnerId, boolean available) {
        User partner = userRepository.findById(partnerId)
            .orElseThrow(() -> new RuntimeException("Partner not found"));
        partner.setIsAvailable(available);
        return userRepository.save(partner);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getNearestPartners(double lat, double lng) {
        return userRepository.findNearestAvailablePartners(lat, lng);
    }
}
