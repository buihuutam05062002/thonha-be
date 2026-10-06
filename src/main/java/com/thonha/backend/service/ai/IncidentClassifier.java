package com.thonha.backend.service.ai;
import com.thonha.backend.entity.ServiceCategory;

import java.util.List;

/**
 * Phân loại sự cố từ mô tả + ảnh thành một ServiceCategory.
 * Chỉ định nghĩa "hợp đồng"; muốn đổi Gemini sang Groq/OpenRouter chỉ cần viết thêm một implementation khác.
 */
public interface IncidentClassifier {

    record ImageInput(String mimeType, byte[] bytes) {
    }

    /**
     * @param categoryId null nếu AI không đủ tự tin hoặc không khả dụng -> frontend để khách tự chọn
     */
    record Result(Long categoryId, String categoryName, double confidence, String reason) {
        public static Result none(String reason) {
            return new Result(null, null, 0, reason);
        }
    }

    Result classify(String description, List<ImageInput> images, List<ServiceCategory> categories);
}