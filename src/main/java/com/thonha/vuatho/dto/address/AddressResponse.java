package com.thonha.vuatho.dto.address;

import com.thonha.vuatho.entity.Address;

import java.math.BigDecimal;

public record AddressResponse(Long id, String label, String fullAddress, BigDecimal lat, BigDecimal lng,
                              boolean defaultAddress) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getFullAddress(), a.getLat(), a.getLng(), a.isDefaultAddress());
    }
}
