package com.fooddelivery.service;

import com.fooddelivery.model.User;
import com.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository  userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public User register(String name, String email, String password, String phone, String address) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }
        /*
         * FIX: Lombok @Builder ignores field-level defaults (= LocalDateTime.now()).
         * Must set createdAt explicitly in the builder chain, otherwise it saves NULL.
         */
        User user = User.builder()
            .name(name)
            .email(email)
            .password(passwordEncoder.encode(password))
            .phone(phone)
            .address(address)
            .role(User.Role.CUSTOMER)
            .isAvailable(true)
            .createdAt(LocalDateTime.now())   // ← EXPLICIT — builder ignores field defaults
            .build();
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateProfile(Long userId, String name, String phone, String address) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(name);
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }
}
