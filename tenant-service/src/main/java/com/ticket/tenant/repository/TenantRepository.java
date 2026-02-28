package com.ticket.tenant.repository;

import com.ticket.tenant.entity.Tenant;
import com.ticket.tenant.entity.Tenant.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Tenant entity operations.
 */
public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findBySchemaName(String schemaName);

    boolean existsBySubdomain(String subdomain);

    boolean existsBySchemaName(String schemaName);

    List<Tenant> findByStatus(TenantStatus status);

    @Query("SELECT t FROM Tenant t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Tenant> findActiveTenants(@Param("status") TenantStatus status);
}
