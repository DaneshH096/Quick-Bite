package com.fooddelivery.config;

import com.fooddelivery.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(
                    "/", "/login", "/register",
                    "/css/**", "/js/**", "/images/**", "/uploads/**",
                    "/error", "/error/**",
                    // Forgot/reset password — must be public (user is logged out)
                    "/forgot-password", "/reset-password"
                ).permitAll()
                .requestMatchers("/api/restaurants/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Delivery partner portal
                .requestMatchers("/delivery/**").hasAnyRole("ADMIN", "DELIVERY_PARTNER")
                // Restaurant-owner portal — manage own restaurant, menu, orders
                .requestMatchers("/restaurant-portal/**").hasAnyRole("ADMIN", "RESTAURANT_OWNER")
                // Everything else needs auth
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> {
                    String role = auth.getAuthorities().stream()
                        .findFirst().map(a -> a.getAuthority()).orElse("");
                    if      (role.equals("ROLE_ADMIN"))            res.sendRedirect("/admin");
                    else if (role.equals("ROLE_DELIVERY_PARTNER")) res.sendRedirect("/delivery");
                    else if (role.equals("ROLE_RESTAURANT_OWNER")) res.sendRedirect("/restaurant-portal");
                    else                                           res.sendRedirect("/home");
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                // Redirect to login (not 403 page) when unauthenticated
                .authenticationEntryPoint((req, res, authEx) -> res.sendRedirect("/login"))
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }
}

