package com.automationedge.apedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ap_field_config")
@Getter
@Setter
public class FieldConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    private Integer tenantId;

    private String fieldName;

    private String fieldType;

    private String parent;

    private String description;

    @Column(name = "use_llm")
    private String useLlm;
}