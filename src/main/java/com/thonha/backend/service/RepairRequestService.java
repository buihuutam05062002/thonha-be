package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateRepairRequest;
import com.thonha.backend.dto.response.RepairRequestResponse;
import com.thonha.backend.entity.*;
import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.*;
import com.thonha.backend.service.MatchingService.MatchingResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RepairRequestService {
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final AddressRepository addressRepository;
    private final CloudinaryService cloudinaryService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;
    private final WorkerProfileRepository workerProfileRepository;
    private final MatchingLogRepository matchingLogRepository;

    public RepairRequestService(RepairRequestRepository repairRequestRepository,
                                UserRepository userRepository,
                                ServiceCategoryRepository serviceCategoryRepository,
                                AddressRepository addressRepository,
                                CloudinaryService cloudinaryService,
                                MatchingService matchingService,
                                NotificationService notificationService,
                                WorkerProfileRepository workerProfileRepository,
                                MatchingLogRepository matchingLogRepository) {
        this.repairRequestRepository = repairRequestRepository;
        this.userRepository = userRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.addressRepository = addressRepository;
        this.cloudinaryService = cloudinaryService;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
        this.workerProfileRepository = workerProfileRepository;
        this.matchingLogRepository = matchingLogRepository;
    }

    @Transactional
    public ApiResponse<RepairRequestResponse> create(Long userId, CreateRepairRequest request, List<MultipartFile> files) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.USER_NOT_FOUND));

        ServiceCategory category = serviceCategoryRepository.findByIdAndStatus(request.getCategoryId(), CategoryStatus.ACTIVE)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND,
                        "Không tìm thấy danh mục dịch vụ"));

        Address address = request.getAddressId() == null ? null :
                addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                        .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                                com.thonha.backend.common.ErrorCode.NOT_FOUND,
                                "Không tìm thấy địa chỉ"));

        if (request.getDesiredTime() == CreateRepairRequest.DesiredTime.SCHEDULED
                && (request.getScheduledAt() == null || !request.getScheduledAt().isAfter(LocalDateTime.now()))) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_REQUEST,
                    "Thời gian hẹn phải là trong tương lai");
        }

        RepairRequest requestEntity = new RepairRequest();
        requestEntity.setCustomer(customer);
        requestEntity.setCategory(category);
        requestEntity.setAddress(address);
        requestEntity.setDescription(request.getDescription().trim());
        requestEntity.setPriorityLevel(request.getPriorityLevel());
        requestEntity.setAddressText(request.getAddressText().trim());
        requestEntity.setLat(request.getLat() != null ? BigDecimal.valueOf(request.getLat()) : null);
        requestEntity.setLng(request.getLng() != null ? BigDecimal.valueOf(request.getLng()) : null);
        requestEntity.setStatus(RepairStatus.PENDING_MATCH);
        // request_code là NOT NULL + UNIQUE nhưng mã thật (VT-xxxx) cần id sau khi insert -> gán mã tạm duy nhất trước
        requestEntity.setRequestCode("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));

        RepairRequest saved = repairRequestRepository.saveAndFlush(requestEntity);
        saved.setRequestCode(String.format("VT-%04d", saved.getId()));

        if (files != null) {
            int sortOrder = 1;
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                RequestAttachment attachment = new RequestAttachment();
                attachment.setRequest(saved);
                attachment.setType(file.getContentType() != null && file.getContentType().startsWith("video") ? "VIDEO" : "IMAGE");
                attachment.setUrl(cloudinaryService.upload(file, "nhatho/requests/" + userId));
                attachment.setSortOrder(sortOrder++);
                saved.getAttachments().add(attachment);
            }
        }
        repairRequestRepository.save(saved);

        // Tự động tìm và match thợ nếu có tọa độ
        MatchingService.MatchingResultDTO matchingResult = null;
        if (saved.getLat() != null && saved.getLng() != null) {
            matchingService.findAndMatchWorkers(saved.getId());
        }

        RepairRequestResponse response = RepairRequestResponse.from(saved);
        return ApiResponse.success(response, "Tạo yêu cầu sửa chữa thành công");
    }

    @Transactional
    public ApiResponse<RepairRequestResponse> assignWorker(Long requestId, Long workerId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        WorkerProfile worker = workerProfileRepository.findById(workerId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.WORKER_NOT_FOUND));

        if (request.getStatus() != RepairStatus.PENDING_MATCH && request.getStatus() != RepairStatus.MATCHING) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_STATUS_TRANSITION,
                    "Yêu cầu không ở trạng thái chờ ghép thợ");
        }

        request.setWorker(worker);
        request.setStatus(RepairStatus.MATCHED);
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);

        notificationService.sendWorkerAccepted(request.getCustomer().getId(), request.getId(), worker.getUser().getFullName());

        return ApiResponse.success(RepairRequestResponse.from(request), "Ghép thợ thành công");
    }

    @Transactional
    public ApiResponse<RepairRequestResponse> cancelRequest(Long userId, Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdAndCustomerId(requestId, userId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (request.getStatus() == RepairStatus.COMPLETED || request.getStatus() == RepairStatus.CANCELLED) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể hủy yêu cầu ở trạng thái này");
        }

        request.setStatus(RepairStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        repairRequestRepository.save(request);

        if (request.getWorker() != null) {
            // thợ đã nhận đơn -> báo cho THỢ biết khách hủy (trước đây gửi nhầm về chính khách)
            notificationService.sendRequestCancelled(request.getWorker().getUser().getId(), request.getId());
        }
        // các thợ đang được mời nhưng chưa phản hồi -> bỏ yêu cầu khỏi dashboard của họ
        matchingLogRepository.findByRequestIdAndResultList(request.getId(), com.thonha.backend.enums.MatchingResult.PENDING)
                .forEach(l -> notificationService.sendMatchingClosed(l.getWorker().getUser().getId(), request.getId()));

        return ApiResponse.success(RepairRequestResponse.from(request), "Hủy yêu cầu thành công");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<RepairRequestResponse>> getMyRequests(Long userId) {
        List<RepairRequestResponse> requests = repairRequestRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(RepairRequestResponse::from)
                .toList();
        return ApiResponse.success(requests);
    }

    @Transactional(readOnly = true)
    public ApiResponse<RepairRequestResponse> getDetail(Long id, Long userId) {
        RepairRequest request = repairRequestRepository.findByIdAndCustomerId(id, userId)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND, "Không tìm thấy yêu cầu sửa chữa"));
        return ApiResponse.success(RepairRequestResponse.from(request));
    }

    @Transactional(readOnly = true)
    public ApiResponse<org.springframework.data.domain.Page<RepairRequestResponse>> getAllRequests(com.thonha.backend.enums.RepairStatus status, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<RepairRequest> page = (status != null)
                ? repairRequestRepository.findByStatus(status, pageable)
                : repairRequestRepository.findAll(pageable);
        return ApiResponse.success(page.map(RepairRequestResponse::from));
    }
}