package com.thonha.vuatho.controller.admin;

import com.thonha.vuatho.entity.*;
import com.thonha.vuatho.repository.ServiceCategoryRepository;
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
        ServiceCategory c = r.findById(id).orElseThrow();
        c.setName(x.getName());
        c.setDescription(x.getDescription());
        c.setIcon(x.getIcon());
        c.setStatus(x.getStatus());
        return r.save(c);
    }

    @PatchMapping("/{id}/status")
    ServiceCategory status(@PathVariable Long id, @RequestParam CategoryStatus status) {
        ServiceCategory c = r.findById(id).orElseThrow();
        c.setStatus(status);
        return r.save(c);
    }
}
