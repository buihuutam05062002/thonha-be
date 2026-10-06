package com.thonha.backend.service;

import com.thonha.backend.dto.request.CreateRepairRequest;
import com.thonha.backend.entity.*;
import com.thonha.backend.exception.*;
import com.thonha.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class RepairRequestService {
    private final RepairRequestRepository requests;
    private final UserRepository users;
    private final ServiceCategoryRepository categories;
    private final AddressRepository addresses;
    private final CloudinaryService cloudinary;

    public RepairRequestService(RepairRequestRepository r, UserRepository u, ServiceCategoryRepository c, AddressRepository a, CloudinaryService cl) {
        requests = r;
        users = u;
        categories = c;
        addresses = a;
        cloudinary = cl;
    }

    @Transactional
    public Map<String, Object> create(Long uid, CreateRepairRequest d, List<MultipartFile> files) {
        User customer = users.findById(uid).orElseThrow(() -> new UnauthorizedException("Account not found"));
        ServiceCategory cat = categories.findByIdInAndStatus(List.of(d.categoryId()), CategoryStatus.ACTIVE).stream().findFirst().orElseThrow(() -> new BadRequestException("Service category not found"));
        Address address = d.addressId() == null ? null : addresses.findByIdAndUserId(d.addressId(), uid).orElseThrow(() -> new BadRequestException("Address not found"));
        // Quyết định của nhóm: thợ chỉ đến ngay lập tức, không hỗ trợ hẹn giờ.
        // Chặn ở server để không thể lách bằng cách gọi API trực tiếp.
        if (d.desiredTime() == CreateRepairRequest.DesiredTime.SCHEDULED)
            throw new BadRequestException("Hệ thống hiện chỉ hỗ trợ thợ đến ngay lập tức, không hỗ trợ hẹn giờ");
        RepairRequest r = new RepairRequest();
        r.setRequestCode("TMP");
        r.setCustomer(customer);
        r.setCategory(cat);
        r.setAddress(address);
        r.setDescription(d.description().trim());
        r.setPriorityLevel(d.priorityLevel());
        r.setAddressText(d.addressText().trim());
        r.setLat(d.lat());
        r.setLng(d.lng());
        r.setStatus(RepairRequest.RepairStatus.PENDING);
        r = requests.saveAndFlush(r);
        r.setRequestCode(String.format("VT-%04d", r.getId()));
        if (files != null) {
            int i = 1;
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                RequestAttachment a = new RequestAttachment();
                a.setRequest(r);
                a.setType(f.getContentType() != null && f.getContentType().startsWith("video") ? "VIDEO" : "IMAGE");
                a.setUrl(cloudinary.upload(f, "vuatho/requests/" + uid));
                a.setSortOrder(i++);
                r.getAttachments().add(a);
            }
        }
        requests.save(r);
        return toMap(r);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(Long uid) {
        return requests.findByCustomerIdOrderByCreatedAtDesc(uid).stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, Long uid) {
        RepairRequest r = requests.findByIdAndCustomerId(id, uid).orElseThrow(() -> new NotFoundException("Repair request not found"));
        return toMap(r);
    }

    private Map<String, Object> toMap(RepairRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("requestCode", r.getRequestCode());
        m.put("category", r.getCategory().getName());
        m.put("description", r.getDescription());
        m.put("status", r.getStatus().name());
        m.put("priorityLevel", r.getPriorityLevel().name());
        m.put("addressText", r.getAddressText());
        m.put("lat", r.getLat());
        m.put("lng", r.getLng());
        m.put("createdAt", r.getCreatedAt());
        m.put("attachmentUrls", r.getAttachments().stream().map(RequestAttachment::getUrl).toList());
        return m;
    }
}