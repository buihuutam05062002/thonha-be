package com.thonha.backend.controller;

import com.thonha.backend.dto.address.*;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService s;
    private final CurrentUserProvider c;

    public AddressController(AddressService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    @GetMapping
    List<AddressResponse> list() {
        return s.list(c.requireUserId());
    }

    @PostMapping
    ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest r) {
        return ResponseEntity.status(201).body(s.create(c.requireUserId(), r));
    }

    @PutMapping("/{id}")
    AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest r) {
        return s.update(c.requireUserId(), id, r);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        s.delete(c.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    AddressResponse def(@PathVariable Long id) {
        return s.setDefault(c.requireUserId(), id);
    }
}
