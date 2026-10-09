package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Address;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String label;
    private String fullAddress;
    private Double lat;
    private Double lng;
    private Boolean defaultAddress;

    public static AddressResponse from(Address a) {
        if (a == null) return null;
        return AddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .fullAddress(a.getFullAddress())
                .lat(a.getLat() != null ? a.getLat().doubleValue() : null)
                .lng(a.getLng() != null ? a.getLng().doubleValue() : null)
                .defaultAddress(a.getDefaultAddress())
                .build();
    }
}