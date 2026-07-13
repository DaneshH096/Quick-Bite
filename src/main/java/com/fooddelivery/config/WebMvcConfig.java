package com.fooddelivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig — explicitly registers static resource paths.
 *
 * WHY THIS EXISTS:
 *   spring.mvc.throw-exception-if-no-handler-found=true (needed for 404 in @ControllerAdvice)
 *   conflicts with spring.web.resources.add-mappings=false (breaks CSS/JS/images).
 *
 *   Solution: keep add-mappings default (true is fine), but register our static
 *   paths here explicitly so Spring knows /css/**, /js/**, /images/** are valid.
 *   Unknown URLs that don't match any controller OR resource handler will then
 *   throw NoHandlerFoundException → caught by GlobalExceptionHandler → 404 page.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve /css/**, /js/**, /images/** from the static folder
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        // Favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
        // User-uploaded restaurant / dish photos — stored on disk under ./uploads
        // (external, so it survives redeploys of the jar and is writable at runtime).
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
