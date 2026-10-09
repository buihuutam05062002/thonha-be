package com.thonha.backend.controller;

import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.common.BadRequestException;
import com.thonha.backend.repository.ServiceCategoryRepository;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.ai.IncidentClassifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Gợi ý danh mục cho form tạo yêu cầu. Cần đăng nhập (anyRequest().authenticated() trong SecurityConfig). */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_IMAGES = 3;
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;
    private static final int MAX_CALLS_PER_MINUTE = 10;

    private final IncidentClassifier classifier;
    private final ServiceCategoryRepository categories;
    private final CurrentUserProvider current;
    /** userId -> thời điểm các lần gọi gần nhất (giới hạn đơn giản trong bộ nhớ, đủ cho đồ án). */
    private final Map<Long, Deque<Long>> calls = new ConcurrentHashMap<>();

    public AiController(IncidentClassifier classifier, ServiceCategoryRepository categories, CurrentUserProvider current) {
        this.classifier = classifier;
        this.categories = categories;
        this.current = current;
    }

    @PostMapping(value = "/classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    IncidentClassifier.Result classify(@RequestParam(value = "description", defaultValue = "") String description,
                                       @RequestPart(value = "files", required = false) List<MultipartFile> files)
            throws IOException {
        checkRate(current.requireUserId());

        String text = description.strip();
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        List<IncidentClassifier.ImageInput> images = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (images.size() >= MAX_IMAGES || f == null || f.isEmpty()) {
                    continue;
                }
                if (!IMAGE_TYPES.contains(f.getContentType())) {
                    continue; // bỏ qua video/định dạng lạ, chỉ phân tích ảnh
                }
                if (f.getSize() > MAX_IMAGE_BYTES) {
                    throw new BadRequestException("Mỗi ảnh gửi cho AI tối đa 4 MB");
                }
                images.add(new IncidentClassifier.ImageInput(f.getContentType(), f.getBytes()));
            }
        }
        if (text.length() < 5 && images.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập mô tả hoặc thêm ảnh để AI phân tích");
        }
        return classifier.classify(text, images, categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
    }

    private void checkRate(Long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = calls.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 60_000) {
                q.pollFirst();
            }
            if (q.size() >= MAX_CALLS_PER_MINUTE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau ít giây");
            }
            q.addLast(now);
        }
    }
}