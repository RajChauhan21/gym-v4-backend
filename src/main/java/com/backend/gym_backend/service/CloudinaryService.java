package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private GymRepository gymRepository;

    @Transactional
    public ResponseEntity<String> uploadImage(int id, String target, MultipartFile file) {

        try {

            // =========================
            // Validate file
            // =========================
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Image file is required");
            }

            String contentType = file.getContentType();

            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            // Optional: max file size (2 MB)
            long maxSize = 2 * 1024 * 1024;

            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("Image size must be or less than 2 MB");
            }

            // =========================
            // Validate target
            // =========================
            if (target == null || target.trim().isEmpty()) {
                throw new IllegalArgumentException("Target is required");
            }

            String normalizedTarget = target.trim().toLowerCase();

            String folderName;

            Owner owner = null;
            Gym gym = null;

            // =========================
            // Validate DB entity BEFORE upload
            // =========================
            switch (normalizedTarget) {

                case "owner":

                    folderName = "owner-profile-assets";

                    owner = ownerRepository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException("Owner not found with id: " + id));

                    break;

                case "gym":

                    folderName = "gym-profile-assets";

                    gym = gymRepository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException("Please setup your gym first, then try to upload logo"));

                    break;

                default:
                    throw new IllegalArgumentException("Invalid target type");
            }

            String oldImagePublicId = null;

            if ("owner".equals(normalizedTarget)) {
                oldImagePublicId = owner.getImagePublicId();
            } else {
                oldImagePublicId = gym.getImagePublicId();
            }

            // =========================
            // Upload image to Cloudinary
            // =========================
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderName,
                            "public_id", generatePublicId(file.getOriginalFilename()),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );



            String publicId = uploadResult.get("public_id").toString();
            String imageUrl = uploadResult.get("secure_url").toString();

            // =========================
            // Update entity
            // =========================
            if ("owner".equals(normalizedTarget)) {

                owner.setImage(imageUrl);
                owner.setImagePublicId(publicId);
//                9798762526 -
                ownerRepository.save(owner);

            } else {

                gym.setImage(imageUrl);
                gym.setImagePublicId(publicId);
                gymRepository.save(gym);
            }

            try {
                //delete existing image from cloudinary
                if (oldImagePublicId != null && !oldImagePublicId.isBlank()){
                    deleteImage(oldImagePublicId);
                }
            } catch (Exception ex) {
                log.error("Failed to delete old image {}", oldImagePublicId, ex);
            }

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(imageUrl);

        }catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to upload image");
        }
    }

    public ResponseEntity<?> deleteImage(String imagePublicId) throws IOException {
        Map result = cloudinary.uploader().destroy(imagePublicId, ObjectUtils.emptyMap());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    private String generatePublicId(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {
            return "img_" + System.currentTimeMillis();
        }

        // remove extension
        String fileName = originalFilename.replaceFirst("[.][^.]+$", "");

        // replace spaces with underscore
        fileName = fileName.replaceAll("\\s+", "_");

        // keep only valid chars
        fileName = fileName.replaceAll("[^a-zA-Z0-9_-]", "");

        // prevent empty filename
        if (fileName.isBlank()) {
            fileName = "image";
        }

        return "img_" + System.currentTimeMillis() + "_" + fileName;
    }
}
