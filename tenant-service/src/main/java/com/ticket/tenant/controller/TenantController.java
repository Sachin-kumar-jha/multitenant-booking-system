package com.ticket.tenant.controller;

import com.ticket.tenant.dto.CreateTenantRequest;
import com.ticket.tenant.dto.TenantResponse;
import com.ticket.tenant.dto.UpdateTenantRequest;
import com.ticket.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tenant management operations.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Create a new tenant (onboarding).
     * This endpoint is public - used for new tenant registration.
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get tenant by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    /**
     * Get tenant by subdomain (public endpoint for tenant validation).
     */
    @GetMapping("/public/subdomain/{subdomain}")
    public ResponseEntity<TenantResponse> getTenantBySubdomain(@PathVariable String subdomain) {
        return ResponseEntity.ok(tenantService.getTenantBySubdomain(subdomain));
    }

    /**
     * Get all tenants (admin only).
     */
    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    /**
     * Update tenant information.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    /**
     * Suspend a tenant.
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.suspendTenant(id));
    }

    /**
     * Activate a tenant.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<TenantResponse> activateTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantService.activateTenant(id));
    }
}
