package com.thonha.vuatho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerSpecialtyId implements Serializable {

    @Column(name = "worker_profile_id")
    private Long workerProfileId;

    @Column(name = "service_category_id")
    private Long serviceCategoryId;
}