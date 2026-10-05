package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.AvailabilityRequest;
import com.thonha.backend.dto.request.RegisterWorkerProfileRequest;
import com.thonha.backend.dto.response.WorkerProfileResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.*;
import com.thonha.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkerService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final CloudinaryService cloudinaryService;

    public WorkerService(UserRepository userRepository,
                         RoleRepository roleRepository,
                         WorkerProfileRepository workerProfileRepository,
                         ServiceCategoryRepository serviceCategoryRepository,
                         CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public WorkerProfileResponse register(Long userId, RegisterWorkerProfileRequest request,
                                          MultipartFile cccdFront, MultipartFile cccdBack,
                                          List<MultipartFile> certificates, List<MultipartFile> degrees) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (workerProfileRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.WORKER_PROFILE_EXISTS);
        }
        if (cccdFront == null || cccdFront.isEmpty() || cccdBack == null || cccdBack.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "CCCD mặt trước và mặt sau là bắt buộc");
        }

        Set<Long> categoryIds = request.getCategoryIds().stream().collect(Collectors.toSet());
        List<ServiceCategory> categories = serviceCategoryRepository.findByIdInAndStatus(categoryIds, CategoryStatus.ACTIVE);
        if (categories.size() != categoryIds.size()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều chuyên môn không hợp lệ hoặc không hoạt động");
        }

        WorkerProfile profile = WorkerProfile.builder()
                .user(userRepository.getReferenceById(userId))
                .provinceCity(request.getProvinceCity().trim())
                .operatingArea(request.getOperatingArea().trim())
                .experienceYears(request.getExperienceYears())
                .build();

        categories.forEach(profile::addSpecialty);

        String folder = "nhatho/workers/" + userId;
        profile.addDocument(DocumentType.CCCD_FRONT, cloudinaryService.upload(cccdFront, folder));
        profile.addDocument(DocumentType.CCCD_BACK, cloudinaryService.upload(cccdBack, folder));

        if (certificates != null) {
            for (MultipartFile file : certificates) {
                if (file != null && !file.isEmpty()) {
                    profile.addDocument(DocumentType.CERTIFICATE, cloudinaryService.upload(file, folder));
                }
            }
        }
        if (degrees != null) {
            for (MultipartFile file : degrees) {
                if (file != null && !file.isEmpty()) {
                    profile.addDocument(DocumentType.DEGREE, cloudinaryService.upload(file, folder));
                }
            }
        }

        workerProfileRepository.save(profile);

        if (!user.hasRole(Role.WORKER)) {
            user.getRoles().add(getRole(Role.WORKER));
        }

        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }

    public WorkerProfileResponse getProfile(Long userId) {
        return workerProfileRepository.findByUserId(userId)
                .map(WorkerProfileResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
    }

    public WorkerProfileResponse updateAvailability(Long userId, AvailabilityRequest request) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));

        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(ErrorCode.WORKER_NOT_APPROVED);
        }
        if (profile.getOngoingJobsCount() > 0) {
            throw new ApiException(ErrorCode.WORKER_HAS_ONGOING_JOBS);
        }

        profile.setAvailabilityStatus(request.isAvailable() ? AvailabilityStatus.READY : AvailabilityStatus.OFFLINE);
        return WorkerProfileResponse.from(workerProfileRepository.save(profile));
    }

    private Role getRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not configured: " + name));
    }
}