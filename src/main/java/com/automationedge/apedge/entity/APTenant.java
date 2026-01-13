package com.automationedge.apedge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ap_tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class APTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Integer tenantId;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "logo_path")
    private String logoPath;
}
