package com.thonha.vuatho.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${app.cloudinary.url:}") String cloudinaryUrl
    ) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            this.cloudinary = null;
        } else {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
        }
    }

    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        try {
            var result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );

            return result.get("secure_url").toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not upload document",
                    e
            );
        }
    }
}