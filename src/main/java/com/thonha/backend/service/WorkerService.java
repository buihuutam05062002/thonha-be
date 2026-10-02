package com.thonha.backend.service;

import com.thonha.backend.dto.worker.*;
import com.thonha.backend.entity.*;
import com.thonha.backend.exception.*;
import com.thonha.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class WorkerService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final WorkerProfileRepository profiles;
    private final ServiceCategoryRepository categories;
    private final CloudinaryService cloudinary;

    public WorkerService(UserRepository u, RoleRepository r, WorkerProfileRepository p, ServiceCategoryRepository c, CloudinaryService cl) {
        users = u;
        roles = r;
        profiles = p;
        categories = c;
        cloudinary = cl;
    }

    @Transactional
    public WorkerProfileResponse register(Long uid, RegisterWorkerProfileRequest req, MultipartFile front, MultipartFile back, List<MultipartFile> certificates, List<MultipartFile> degrees) {
        User u = users.findById(uid).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getStatus() == UserStatus.LOCKED) throw new BadRequestException("Account is locked");
        if (profiles.existsByUserId(uid)) throw new BadRequestException("Worker profile already exists");
        if (front == null || back == null || front.isEmpty() || back.isEmpty())
            throw new BadRequestException("CCCD front and back are required");
        Set<Long> ids = new HashSet<>(req.categoryIds());
        List<ServiceCategory> cs = categories.findByIdInAndStatus(ids, CategoryStatus.ACTIVE);
        if (cs.size() != ids.size()) throw new BadRequestException("One or more service categories are invalid");
        WorkerProfile p = new WorkerProfile();
        p.setUser(u);
        p.setProvinceCity(req.provinceCity().trim());
        p.setOperatingArea(req.operatingArea().trim());
        p.setExperienceYears(req.experienceYears());
        cs.forEach(p::addSpecialty);
        String folder = "vuatho/workers/" + uid;
        p.addDocument(DocumentType.CCCD_FRONT, cloudinary.upload(front, folder));
        p.addDocument(DocumentType.CCCD_BACK, cloudinary.upload(back, folder));
        for (var f : safe(certificates)) p.addDocument(DocumentType.CERTIFICATE, cloudinary.upload(f, folder));
        for (var f : safe(degrees)) p.addDocument(DocumentType.DEGREE, cloudinary.upload(f, folder));
        profiles.save(p);
        if (!u.hasRole(Role.WORKER))
            u.getRoles().add(roles.findByName(Role.WORKER).orElseThrow(() -> new IllegalStateException("WORKER role not configured")));
        return WorkerProfileResponse.from(p);
    }

    @Transactional(readOnly = true)
    public WorkerProfileResponse me(Long uid) {
        return WorkerProfileResponse.from(profiles.findByUserId(uid).orElseThrow(() -> new NotFoundException("Worker profile not found")));
    }

    @Transactional
    public WorkerProfileResponse updateAvailability(Long uid, boolean available) {
        WorkerProfile p = profiles.findByUserId(uid).orElseThrow(() -> new NotFoundException("Worker profile not found"));
        if (p.getApprovalStatus() != ApprovalStatus.APPROVED)
            throw new BadRequestException("Worker profile is not approved");
        if (p.getOngoingJobsCount() > 0) throw new BadRequestException("Worker has ongoing jobs");
        p.setAvailabilityStatus(available ? AvailabilityStatus.READY : AvailabilityStatus.OFFLINE);
        return WorkerProfileResponse.from(p);
    }

    private List<MultipartFile> safe(List<MultipartFile> x) {
        return x == null ? List.of() : x.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }
}
