package com.thonha.backend.controller.admin;

import com.thonha.backend.entity.*;
import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.repository.ServiceCategoryRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/service-categories")
public class CategoryAdminController {
    private final ServiceCategoryRepository r;

    public CategoryAdminController(ServiceCategoryRepository r) {
        this.r = r;
    }

    @PostMapping
    ServiceCategory create(@RequestBody ServiceCategory c) {
        c.setId(null);
        return r.save(c);
    }

    @PutMapping("/{id}")
    ServiceCategory update(@PathVariable Long id, @RequestBody ServiceCategory x) {
        ServiceCategory c = r.findById(id).orElseThrow(() -> new com.thonha.backend.common.NotFoundException("Category not found"));
        c.setName(x.getName());
        c.setDescription(x.getDescription());
        c.setIcon(x.getIcon());
        c.setStatus(x.getStatus());
        return r.save(c);
    }

    @PatchMapping("/{id}/status")
    ServiceCategory status(@PathVariable Long id, @RequestParam CategoryStatus status) {
        ServiceCategory c = r.findById(id).orElseThrow(() -> new com.thonha.backend.common.NotFoundException("Category not found"));
        c.setStatus(status);
        return r.save(c);
    }
}
