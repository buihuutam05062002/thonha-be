package com.thonha.vuatho.controller;

import com.thonha.vuatho.dto.request.CreateRepairRequest;
import com.thonha.vuatho.security.CurrentUserProvider;
import com.thonha.vuatho.service.RepairRequestService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/repair-requests")
public class RepairRequestController {
    private final RepairRequestService s;
    private final CurrentUserProvider c;

    public RepairRequestController(RepairRequestService s, CurrentUserProvider c) {
        this.s = s;
        this.c = c;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Map<String, Object>> create(@Valid @RequestPart("data") CreateRepairRequest data, @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.status(201).body(s.create(c.requireUserId(), data, files));
    }

    @GetMapping
    List<Map<String, Object>> mine() {
        return s.mine(c.requireUserId());
    }

    @GetMapping("/{id}")
    Map<String, Object> detail(@PathVariable Long id) {
        return s.detail(id, c.requireUserId());
    }
}
