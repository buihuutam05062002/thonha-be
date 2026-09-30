package com.thonha.backend.dto.worker;

import com.thonha.backend.entity.ApprovalStatus;
import com.thonha.backend.entity.AvailabilityStatus;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.WorkerProfile;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record WorkerProfileResponse(
        Long id,
        String provinceCity,
        String operatingArea,
        Integer experienceYears,
        ApprovalStatus approvalStatus,
        AvailabilityStatus availabilityStatus,
        List<Specialty> specialties,
        Set<String> role
){
    public record Specialty(Long id, String name){
    }
    
    public static WorkerProfileResponse from(WorkerProfile profile){
        List<Specialty> specialties = profile.getSpecialties().stream()
                .map(t -> new Specialty(t.getServiceCategory().getId(), t.getServiceCategory().getName()))
                .sorted(Comparator.comparing(Specialty::id))
                .toList();
        Set<String> role = profile.getUser().getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new WorkerProfileResponse(
                profile.getId(),
                profile.getProvinceCity(),
                profile.getOperatingArea(),
                profile.getExperienceYears(),
                profile.getApprovalStatus(),
                profile.getAvailabilityStatus(),
                specialties,
                role
        );
    }
}
