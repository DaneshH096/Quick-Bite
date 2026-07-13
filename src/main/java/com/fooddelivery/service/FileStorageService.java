package com.fooddelivery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves uploaded photos (restaurant photos, dish photos) to an "uploads" folder
 * on disk, next to the running application, and returns a public "/uploads/..."
 * URL that WebMvcConfig serves back out.
 *
 * Kept deliberately simple (local disk) — swap for S3/Cloud storage later
 * without touching any calling code, since callers only see the returned URL.
 */
@Service
public class FileStorageService {

    private static final String UPLOAD_ROOT = "uploads";

    /**
     * @param file      the uploaded photo (may be null/empty — caller should
     *                  fall back to a manually-typed image URL in that case)
     * @param subFolder logical bucket, e.g. "restaurants" or "menu-items"
     * @return public URL such as /uploads/menu-items/ab12cd34-photo.jpg
     */
    public String store(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path dir = Paths.get(UPLOAD_ROOT, subFolder);
            Files.createDirectories(dir);

            String original = Paths.get(file.getOriginalFilename() == null
                ? "photo" : file.getOriginalFilename()).getFileName().toString();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedName = UUID.randomUUID().toString().substring(0, 8) + "-" + safeName;

            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subFolder + "/" + storedName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file: " + e.getMessage(), e);
        }
    }
}
