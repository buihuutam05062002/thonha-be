package com.thonha.backend.service.worker;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.dto.worker.RegisterWorkerProfileRequest;
import com.thonha.backend.dto.worker.WorkerProfileResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.repository.category.ServiceCategoryRepository;
import com.thonha.backend.repository.user.RoleRepository;
import com.thonha.backend.repository.user.UserRepository;
import com.thonha.backend.repository.worker.WorkerProfileRepository;
import com.thonha.backend.service.cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkerProfileService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public WorkerProfileResponse register(Long userId,
                                          RegisterWorkerProfileRequest req,
                                          MultipartFile cccdFront,
                                          MultipartFile cccdBack,
                                          List<MultipartFile> certificates,
                                          List<MultipartFile> degrees) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"
                ));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_LOCKED", "Tài khoản đang bị khóa");
        }

        if (workerProfileRepository.existsByUserId(userId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "WORKER_PROFILE_EXISTS", "Bạn đã đăng ký hồ sơ thợ rồi"
            );
        }

        Set<Long> ids = new HashSet<>(req.categoryIds());
        List<ServiceCategory> categories = serviceCategoryRepository.findByIdInAndStatus(ids, CategoryStatus.ACTIVE);
        if (categories.size() != ids.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "Có chuyên môn không tồn tại hoặc đã ngừng hoạt động"
            );
        }

        if (isEmpty(cccdFront) || isEmpty(cccdBack)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_DOCUMENT",
                    "Vui lòng tải lên ảnh CCCD mặt trước và mặt sau");
        }
        List<MultipartFile> certList = nonEmpty(certificates);
        List<MultipartFile> degreeList = nonEmpty(degrees);
        if (certList.size() > 5 || degreeList.size() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_FILES",
                    "Mỗi loại giấy tờ tối đa 5 file");
        }

        WorkerProfile profile = new WorkerProfile();
        profile.setUser(user);
        profile.setProvinceCity(req.provinceCity().trim());
        profile.setOperatingArea(req.operatingArea().trim());
        profile.setExperienceYears(req.experienceYears());
        categories.forEach(profile::addSpecialty);
        String folder = "thonha/workers/" + userId;
        profile.addDocument(DocumentType.CCCD_FRONT, cloudinaryService.uploadDocument(cccdFront, folder));
        profile.addDocument(DocumentType.CCCD_BACK, cloudinaryService.uploadDocument(cccdBack, folder));
        certList.forEach(f->profile.addDocument(DocumentType.CERTIFICATE, cloudinaryService.uploadDocument(f, folder)));
        degreeList.forEach(f->profile.addDocument(DocumentType.DEGREE, cloudinaryService.uploadDocument(f, folder)));
        workerProfileRepository.save(profile);  

        if (!user.hasRole(Role.WORKER)) {
            Role roleWorker = roleRepository.findByName(Role.WORKER)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_NOT_CONFIGURED", "Chưa cấu hình vai trò Thợ trong hệ thống"
                    ));
            user.getRoles().add(roleWorker);
        }
        return WorkerProfileResponse.from(profile);
    }

    private boolean isEmpty(MultipartFile f) {
        return f == null || f.isEmpty();
    }

    private List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        if (files == null) return List.of();
        return files.stream().filter(f -> !f.isEmpty()).toList();
    }
    
    @Transactional(readOnly = true)
    public WorkerProfileResponse getMyProfile(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "WORKER_PROFILE_NOT_FOUND", "Bạn chưa đăng ký hồ sơ thợ"));
        return WorkerProfileResponse.from(profile);
    }
    
    @Transactional
    public WorkerProfileResponse updateAvailability(Long userId, boolean available){
        WorkerProfile profile = workerProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND, "WORKER_PROFILE_NOT_FOUND", "Bạn chưa đăng ký hồ sơ thợ"));
        if (profile.getUser().getStatus() == UserStatus.LOCKED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_LOCKED", "Tài khoản đang bị khóa");
        }
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKER_NOT_APPROVED",
                    "Hồ sơ của bạn chưa được duyệt, chưa thể bật nhận việc");
        }
        if (profile.getAvailabilityStatus() == AvailabilityStatus.BUSY
                || profile.getOngoingJobsCount() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKER_BUSY",
                    "Bạn đang có đơn đang thực hiện, không thể thay đổi trạng thái nhận việc");
        }
        profile.setAvailabilityStatus(available ? AvailabilityStatus.READY : AvailabilityStatus.OFFLINE);
        return WorkerProfileResponse.from(profile);
    }
}
