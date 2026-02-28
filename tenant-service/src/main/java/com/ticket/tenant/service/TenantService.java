package com.ticket.tenant.service;

import com.ticket.common.event.TenantCreatedEvent;
import com.ticket.common.exception.DuplicateRequestException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.redis.RedisLockService;
import com.ticket.common.tenant.RedisTenantRegistry;
import com.ticket.common.tenant.TenantInfo;
import com.ticket.tenant.dto.CreateTenantRequest;
import com.ticket.tenant.dto.TenantResponse;
import com.ticket.tenant.dto.UpdateTenantRequest;
import com.ticket.tenant.entity.Tenant;
import com.ticket.tenant.entity.Tenant.TenantStatus;
import com.ticket.tenant.repository.TenantRepository;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tenant management and onboarding.
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private static final String SCHEMA_PREFIX = "tenant_";
    private static final Duration SCHEMA_CREATION_LOCK_TTL = Duration.ofMinutes(5);

    private final TenantRepository repository;
    private final DataSource dataSource;
    private final RedisLockService lockService;
    private final RedisTenantRegistry tenantRegistry;
    private final EventPublisher eventPublisher;

    @Value("${spring.flyway.locations:classpath:db/migration/tenant}")
    private String flywayLocations;

    public TenantService(
            TenantRepository repository,
            DataSource dataSource,
            RedisLockService lockService,
            RedisTenantRegistry tenantRegistry,
            EventPublisher eventPublisher) {
        this.repository = repository;
        this.dataSource = dataSource;
        this.lockService = lockService;
        this.tenantRegistry = tenantRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String subdomain = normalizeSubdomain(request.getSubdomain());
        String schemaName = SCHEMA_PREFIX + subdomain;
        String lockKey = "tenant-creation:" + subdomain;
        String lockValue = UUID.randomUUID().toString();

        // Acquire distributed lock to prevent race conditions
        if (!lockService.acquireLock(lockKey, lockValue, SCHEMA_CREATION_LOCK_TTL)) {
            throw new DuplicateRequestException("Tenant creation already in progress: " + subdomain);
        }

        try {
            // Check for existing tenant
            if (repository.existsBySubdomain(subdomain)) {
                throw new DuplicateRequestException("Tenant with subdomain already exists: " + subdomain);
            }

            // Create tenant record
            Tenant tenant = new Tenant();
            tenant.setId(UUID.randomUUID().toString());
            tenant.setName(request.getName());
            tenant.setSubdomain(subdomain);
            tenant.setSchemaName(schemaName);
            tenant.setAdminEmail(request.getAdminEmail());
            tenant.setPlan(request.getPlan() != null ? request.getPlan() : "BASIC");
            tenant.setMaxUsers(getPlanMaxUsers(tenant.getPlan()));
            tenant.setMaxEvents(getPlanMaxEvents(tenant.getPlan()));
            tenant.setStatus(TenantStatus.PENDING);

            Tenant saved = repository.save(tenant);
            log.info("Created tenant record: {} with schema: {}", tenant.getId(), schemaName);

            // Create schema and run migrations
            createSchemaAndMigrate(schemaName);

            // Update tenant status to ACTIVE
            saved.setStatus(TenantStatus.ACTIVE);
            repository.save(saved);

            // Register in Redis cache
            TenantInfo tenantInfo = new TenantInfo(
                    saved.getId(),
                    saved.getName(),
                    saved.getSchemaName(),
                    saved.getStatus().name(),
                    saved.getPlan()
            );
            tenantRegistry.registerTenant(tenantInfo);

            // Publish event
            publishTenantCreatedEvent(saved);

            log.info("Tenant onboarding completed: {}", tenant.getId());
            return mapToResponse(saved);

        } finally {
            lockService.releaseLock(lockKey, lockValue);
        }
    }

    private void createSchemaAndMigrate(String schemaName) {
        log.info("Creating schema and running migrations: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(flywayLocations)
                .baselineOnMigrate(true)
                .createSchemas(true)
                .load();

        flyway.migrate();

        log.info("Schema created and migrations completed: {}", schemaName);
    }

    private void publishTenantCreatedEvent(Tenant tenant) {
        TenantCreatedEvent event = new TenantCreatedEvent(
                tenant.getSchemaName(),
                UUID.randomUUID().toString(),
                tenant.getName(),
                tenant.getSchemaName(),
                tenant.getAdminEmail(),
                tenant.getPlan()
        );
        
        eventPublisher.publish(KafkaTopics.TENANT_CREATED, event);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(String id) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantBySubdomain(String subdomain) {
        Tenant tenant = repository.findBySubdomain(normalizeSubdomain(subdomain))
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", subdomain));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantResponse updateTenant(String id, UpdateTenantRequest request) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getPlan() != null) {
            tenant.setPlan(request.getPlan());
            tenant.setMaxUsers(getPlanMaxUsers(request.getPlan()));
            tenant.setMaxEvents(getPlanMaxEvents(request.getPlan()));
        }
        if (request.getSettings() != null) {
            tenant.setSettings(request.getSettings());
        }

        Tenant saved = repository.save(tenant);

        // Update cache
        TenantInfo tenantInfo = new TenantInfo(
                saved.getId(),
                saved.getName(),
                saved.getSchemaName(),
                saved.getStatus().name(),
                saved.getPlan()
        );
        tenantRegistry.registerTenant(tenantInfo);

        return mapToResponse(saved);
    }

    @Transactional
    public TenantResponse suspendTenant(String id) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));

        tenant.setStatus(TenantStatus.SUSPENDED);
        Tenant saved = repository.save(tenant);

        // Update cache
        tenantRegistry.updateTenantStatus(tenant.getSubdomain(), TenantStatus.SUSPENDED.name());

        log.warn("Tenant suspended: {}", id);
        return mapToResponse(saved);
    }

    @Transactional
    public TenantResponse activateTenant(String id) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));

        tenant.setStatus(TenantStatus.ACTIVE);
        Tenant saved = repository.save(tenant);

        // Update cache
        tenantRegistry.updateTenantStatus(tenant.getSubdomain(), TenantStatus.ACTIVE.name());

        log.info("Tenant activated: {}", id);
        return mapToResponse(saved);
    }

    private String normalizeSubdomain(String subdomain) {
        return subdomain.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .substring(0, Math.min(subdomain.length(), 50));
    }

    private int getPlanMaxUsers(String plan) {
        return switch (plan.toUpperCase()) {
            case "ENTERPRISE" -> 10000;
            case "PROFESSIONAL" -> 1000;
            case "BASIC" -> 100;
            default -> 100;
        };
    }

    private int getPlanMaxEvents(String plan) {
        return switch (plan.toUpperCase()) {
            case "ENTERPRISE" -> 500;
            case "PROFESSIONAL" -> 100;
            case "BASIC" -> 50;
            default -> 50;
        };
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSubdomain(tenant.getSubdomain());
        response.setSchemaName(tenant.getSchemaName());
        response.setAdminEmail(tenant.getAdminEmail());
        response.setPlan(tenant.getPlan());
        response.setStatus(tenant.getStatus().name());
        response.setMaxUsers(tenant.getMaxUsers());
        response.setMaxEvents(tenant.getMaxEvents());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }

    // Helper methods for internal use and testing
    public java.util.Optional<com.ticket.tenant.entity.Tenant> findByTenantId(String tenantId) {
        return repository.findBySubdomain(tenantId);
    }

    public boolean validateTenant(String tenantId) {
        return repository.findBySubdomain(tenantId)
                .map(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }

    @Transactional
    public com.ticket.tenant.entity.Tenant suspendTenantBySubdomain(String tenantId) {
        Tenant tenant = repository.findBySubdomain(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRegistry.updateTenantStatus(tenantId, TenantStatus.SUSPENDED.name());
        return repository.save(tenant);
    }

    @Transactional
    public com.ticket.tenant.entity.Tenant activateTenantBySubdomain(String tenantId) {
        Tenant tenant = repository.findBySubdomain(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRegistry.updateTenantStatus(tenantId, TenantStatus.ACTIVE.name());
        return repository.save(tenant);
    }

    @Transactional
    public com.ticket.tenant.entity.Tenant updateSubscription(String tenantId, com.ticket.tenant.entity.SubscriptionPlan plan) {
        Tenant tenant = repository.findBySubdomain(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setPlan(plan.name());
        tenant.setMaxUsers(getPlanMaxUsers(plan.name()));
        tenant.setMaxEvents(getPlanMaxEvents(plan.name()));
        return repository.save(tenant);
    }

    public String generateSchemaName(String name) {
        String normalized = name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return SCHEMA_PREFIX + normalized.substring(0, Math.min(normalized.length(), 40));
    }

    @Transactional
    public com.ticket.tenant.entity.Tenant registerTenant(com.ticket.tenant.dto.TenantRegistrationRequest request) {
        if (repository.existsBySubdomain(request.getTenantId())) {
            throw new com.ticket.common.exception.BusinessException("Tenant ID already exists");
        }

        CreateTenantRequest createRequest = new CreateTenantRequest();
        createRequest.setSubdomain(request.getTenantId());
        createRequest.setName(request.getName());
        createRequest.setAdminEmail(request.getAdminEmail());
        createRequest.setPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan() : "BASIC");

        TenantResponse response = createTenant(createRequest);
        return repository.findById(response.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", response.getId()));
    }
}
