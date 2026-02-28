package com.ticket.tenant.service;

import com.ticket.common.exception.BusinessException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.redis.RedisLockService;
import com.ticket.common.tenant.RedisTenantRegistry;
import com.ticket.tenant.dto.TenantRegistrationRequest;
import com.ticket.tenant.entity.Tenant;
import com.ticket.tenant.entity.Tenant.TenantStatus;
import com.ticket.tenant.entity.SubscriptionPlan;
import com.ticket.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Service Unit Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private RedisTenantRegistry redisTenantRegistry;

    @InjectMocks
    private TenantService tenantService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setId("test-id");
        testTenant.setSubdomain("test-tenant");
        testTenant.setName("Test Organization");
        testTenant.setSchemaName("tenant_test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setPlan("BASIC");
        testTenant.setAdminEmail("admin@test.com");
    }

    @Test
    @DisplayName("Should find tenant by ID")
    void shouldFindTenantById() {
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));

        Optional<Tenant> result = tenantService.findByTenantId("test-tenant");

        assertThat(result).isPresent();
        assertThat(result.get().getSubdomain()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("Should validate tenant exists")
    void shouldValidateTenantExists() {
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));

        boolean result = tenantService.validateTenant("test-tenant");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should invalidate non-existent tenant")
    void shouldInvalidateNonExistentTenant() {
        when(tenantRepository.findBySubdomain("unknown")).thenReturn(Optional.empty());

        boolean result = tenantService.validateTenant("unknown");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should invalidate suspended tenant")
    void shouldInvalidateSuspendedTenant() {
        testTenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));

        boolean result = tenantService.validateTenant("test-tenant");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should suspend tenant")
    void shouldSuspendTenant() {
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant result = tenantService.suspendTenantBySubdomain("test-tenant");

        assertThat(result.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        verify(tenantRepository).save(testTenant);
    }

    @Test
    @DisplayName("Should activate tenant")
    void shouldActivateTenant() {
        testTenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant result = tenantService.activateTenantBySubdomain("test-tenant");

        assertThat(result.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should upgrade subscription")
    void shouldUpgradeSubscription() {
        when(tenantRepository.findBySubdomain("test-tenant")).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(i -> i.getArgument(0));

        Tenant result = tenantService.updateSubscription("test-tenant", SubscriptionPlan.ENTERPRISE);

        assertThat(result.getPlan()).isEqualTo("ENTERPRISE");
    }

    @Test
    @DisplayName("Should generate valid schema name")
    void shouldGenerateSchemaName() {
        String schemaName = tenantService.generateSchemaName("My Company!");

        assertThat(schemaName).startsWith("tenant_");
        assertThat(schemaName).doesNotContain(" ");
        assertThat(schemaName).matches("tenant_[a-z0-9_]+");
    }

    @Test
    @DisplayName("Should reject duplicate tenant ID")
    void shouldRejectDuplicateTenantId() {
        when(tenantRepository.existsBySubdomain("test-tenant")).thenReturn(true);

        TenantRegistrationRequest request = new TenantRegistrationRequest();
        request.setTenantId("test-tenant");
        request.setName("Test");
        request.setAdminEmail("admin@test.com");

        assertThatThrownBy(() -> tenantService.registerTenant(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Tenant ID already exists");
    }
}
