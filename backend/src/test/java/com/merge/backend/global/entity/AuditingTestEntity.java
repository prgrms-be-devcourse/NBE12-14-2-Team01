package com.merge.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auditing_test_entity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AuditingTestEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    AuditingTestEntity(String name) {
        this.name = name;
    }

    void changeName(String name) {
        this.name = name;
    }
}