package com.thonha.vuatho.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_specialty")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSpecialty {

    @EmbeddedId
    private WorkerSpecialtyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workerProfileId")
    @JoinColumn(name = "worker_profile_id")
    private WorkerProfile workerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceCategoryId")
    @JoinColumn(name = "service_category_id")
    private ServiceCategory serviceCategory;
}
