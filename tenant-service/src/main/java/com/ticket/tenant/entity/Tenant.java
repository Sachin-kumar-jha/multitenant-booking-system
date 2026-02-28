package com.ticket.tenant.entity;

import com.ticket.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tenant entity represents a customer organization in the SaaS platform.
 */
@Entity
@Table(name = "tenants", schema = "public",
       indexes = {
           @Index(name = "idx_tenant_subdomain", columnList = "subdomain"),
           @Index(name = "idx_tenant_status", columnList = "status")
       })
public class Tenant extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false, unique = true)
    private String subdomain;

    @NotBlank
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @NotBlank
    @Email
    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(length = 50)
    private String plan = "BASIC";

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status = TenantStatus.PENDING;

    @Column(name = "max_users")
    private Integer maxUsers = 100;

    @Column(name = "max_events")
    private Integer maxEvents = 50;

    @Column(columnDefinition = "TEXT")
    private String settings;

    public Tenant() {}

    public enum TenantStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DELETED
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(Integer maxEvents) {
        this.maxEvents = maxEvents;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }
}
