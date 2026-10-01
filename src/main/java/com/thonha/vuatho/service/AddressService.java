package com.thonha.vuatho.service;

import com.thonha.vuatho.dto.address.*;
import com.thonha.vuatho.entity.*;
import com.thonha.vuatho.exception.*;
import com.thonha.vuatho.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AddressService {
    private final AddressRepository repo;
    private final UserRepository users;

    public AddressService(AddressRepository r, UserRepository u) {
        repo = r;
        users = u;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long uid) {
        return repo.findByUserIdOrderByDefaultAddressDescIdDesc(uid).stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(Long uid, AddressRequest r) {
        Address a = new Address();
        a.setUser(user(uid));
        copy(a, r);
        if (r.defaultAddress() || repo.countByUserId(uid) == 0) {
            clear(uid);
            a.setDefaultAddress(true);
        }
        return AddressResponse.from(repo.save(a));
    }

    @Transactional
    public AddressResponse update(Long uid, Long id, AddressRequest r) {
        Address a = own(uid, id);
        if (r.defaultAddress()) clear(uid);
        copy(a, r);
        a.setDefaultAddress(r.defaultAddress());
        return AddressResponse.from(a);
    }

    @Transactional
    public void delete(Long uid, Long id) {
        Address a = own(uid, id);
        boolean d = a.isDefaultAddress();
        repo.delete(a);
        if (d) repo.findByUserIdOrderByDefaultAddressDescIdDesc(uid).stream().findFirst().ifPresent(x -> {
            x.setDefaultAddress(true);
        });
    }

    @Transactional
    public AddressResponse setDefault(Long uid, Long id) {
        Address a = own(uid, id);
        clear(uid);
        a.setDefaultAddress(true);
        return AddressResponse.from(a);
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new UnauthorizedException("Account not found"));
    }

    private Address own(Long u, Long id) {
        return repo.findByIdAndUserId(id, u).orElseThrow(() -> new NotFoundException("Address not found"));
    }

    private void clear(Long uid) {
        repo.findByUserIdOrderByDefaultAddressDescIdDesc(uid).forEach(x -> x.setDefaultAddress(false));
    }

    private void copy(Address a, AddressRequest r) {
        a.setLabel(r.label() == null ? null : r.label().trim());
        a.setFullAddress(r.fullAddress().trim());
        a.setLat(r.lat());
        a.setLng(r.lng());
    }
}
