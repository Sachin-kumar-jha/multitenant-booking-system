# Multitenant Event-Driven Ticket Booking SaaS Platform

## Architecture Overview

### System Architecture Diagram (Text Form)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    EXTERNAL TRAFFIC                                       │
│                         tenant1.yourapp.com / tenant2.yourapp.com                        │
└─────────────────────────────────────────┬───────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   NGINX (Port 80/443)                                    │
│                    - SSL Termination - Wildcard DNS - Host Header Forwarding             │
└─────────────────────────────────────────┬───────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY (Spring Cloud Gateway)                          │
│            - JWT Validation - Rate Limiting - Tenant Extraction - Routing               │
└─────────────────┬────────────┬──────────────┬─────────────┬─────────────┬───────────────┘
                  │            │              │             │             │
    ┌─────────────┼────────────┼──────────────┼─────────────┼─────────────┼──────────────┐
    │             ▼            ▼              ▼             ▼             ▼              │
    │     ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐         │
    │     │   AUTH    │ │  TENANT   │ │   EVENT   │ │  BOOKING  │ │  PAYMENT  │         │
    │     │  SERVICE  │ │  SERVICE  │ │  SERVICE  │ │  SERVICE  │ │  SERVICE  │         │
    │     └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘         │
    │           │             │             │             │             │               │
    │     INTERNAL DOCKER NETWORK (ticket-network)                      │               │
    │           │             │             │             │             │               │
    │           └──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┘               │
    │                  │             │             │             │                       │
    │                  ▼             ▼             ▼             ▼                       │
    │          ┌───────────────────────────────────────────────────────────┐            │
    │          │                    KAFKA (KRaft Mode)                      │            │
    │          │  Topics: booking.requested, payment.completed, etc.       │            │
    │          └───────────────────────────────────────────────────────────┘            │
    │                                      │                                            │
    │                  ┌───────────────────┼───────────────────┐                        │
    │                  ▼                   ▼                   ▼                        │
    │          ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
    │          │  PostgreSQL │     │    Redis    │     │NOTIFICATION │                  │
    │          │   (Shared)  │     │  (Locking)  │     │   SERVICE   │                  │
    │          └─────────────┘     └─────────────┘     └─────────────┘                  │
    │                                                                                   │
    │          ┌─────────────────────────────────────────────────────────┐              │
    │          │              SERVICE REGISTRY (Eureka)                  │              │
    │          └─────────────────────────────────────────────────────────┘              │
    └───────────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack Decisions

### Why Each Technology Choice

| Technology | Purpose | Rationale |
|------------|---------|-----------|
| **Java 21** | Runtime | Virtual threads, pattern matching, record patterns, improved GC |
| **Spring Boot 3.x** | Framework | Jakarta EE, native compilation support, observability |
| **Spring Cloud Gateway** | API Gateway | Non-blocking, reactive, circuit breaker integration |
| **Spring Security** | Security | Industry standard, OAuth2/JWT support, extensive customization |
| **PostgreSQL** | Primary DB | ACID compliance, schema-based multitenancy, JSON support |
| **Kafka (KRaft)** | Event Bus | High throughput, exactly-once semantics, no Zookeeper dependency |
| **Redis** | Caching/Locking | Sub-millisecond latency, TTL support, distributed locking |
| **Flyway** | Migrations | Version control for schema, repeatable migrations |
| **Resilience4j** | Fault Tolerance | Circuit breaker, rate limiter, bulkhead patterns |
| **Micrometer** | Observability | Prometheus metrics, distributed tracing |

---

## Multitenancy Design

### Schema-Per-Tenant Architecture

```
PostgreSQL Instance: ticket_system
├── public (master schema)
│   └── tenants (tenant registry)
├── tenant_acme (ACME Corp schema)
│   ├── users
│   ├── events
│   ├── seats
│   ├── bookings
│   └── payments
├── tenant_globex (Globex schema)
│   ├── users
│   ├── events
│   ├── seats
│   ├── bookings
│   └── payments
└── ...
```

### Tenant Resolution Flow

```
1. Request arrives: https://acme.yourapp.com/api/events
2. NGINX extracts Host header → acme.yourapp.com
3. API Gateway extracts subdomain → acme
4. TenantFilter validates tenant exists and is ACTIVE
5. TenantContext.setCurrentTenant("tenant_acme")
6. Request proceeds with tenant context
7. MultiTenantConnectionProvider switches to tenant_acme schema
8. Response returns with tenant-specific data
```

### Key Components

1. **TenantContext (ThreadLocal)**: Stores current tenant identifier for request lifecycle
2. **TenantFilter**: Extracts tenant from subdomain, validates, sets context
3. **MultiTenantConnectionProvider**: Switches PostgreSQL schema based on tenant
4. **CurrentTenantIdentifierResolver**: Returns current tenant from TenantContext

---

## Service Responsibilities

### 1. Service Registry (Eureka)
- Service discovery and registration
- Health monitoring
- Load balancing support

### 2. API Gateway
- Single entry point for all external traffic
- JWT validation
- Tenant extraction from subdomain
- Request routing to microservices
- Rate limiting per tenant
- Circuit breaker for downstream services

### 3. Tenant Service
- Tenant onboarding (create tenant record)
- Schema creation via Flyway
- Tenant status management (ACTIVE/SUSPENDED/DELETED)
- Tenant configuration storage
- Publishes TenantCreated events

### 4. Auth Service
- User registration (tenant-scoped)
- JWT token generation with tenant claims
- Token refresh mechanism
- Password hashing (BCrypt)
- Role-based access control

### 5. Event Service
- Event CRUD operations
- Seat map configuration
- Seat inventory management
- Event status management
- Publishes EventCreated, EventCancelled events

### 6. Booking Service (Critical Path)
- Seat selection and locking (Redis)
- Booking request initiation
- Saga orchestration
- Booking confirmation/cancellation
- Compensating transactions
- Publishes BookingRequested, BookingConfirmed, BookingCancelled

### 7. Payment Service
- Payment processing (simulated)
- Idempotent payment handling
- Publishes PaymentCompleted, PaymentFailed

### 8. Notification Service
- Consumes booking/payment events
- Sends notifications (email/SMS simulation)
- Notification template management

---

## Event-Driven Architecture

### Kafka Topics

| Topic | Producer | Consumer(s) | Partition Key |
|-------|----------|-------------|---------------|
| `tenant.created` | Tenant Service | All Services | tenantId |
| `booking.requested` | Booking Service | Payment Service | tenantId |
| `payment.completed` | Payment Service | Booking Service | tenantId |
| `payment.failed` | Payment Service | Booking Service, Notification | tenantId |
| `booking.confirmed` | Booking Service | Notification Service | tenantId |
| `booking.cancelled` | Booking Service | Notification, Payment | tenantId |
| `event.created` | Event Service | Notification Service | tenantId |
| `event.cancelled` | Event Service | Booking, Notification | tenantId |

### Saga Pattern: Booking Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BOOKING SAGA (Orchestrated)                          │
└─────────────────────────────────────────────────────────────────────────────┘

HAPPY PATH:
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Select  │───▶│   Lock   │───▶│  Request │───▶│ Process  │───▶│ Confirm  │
│   Seat   │    │   Seat   │    │  Booking │    │ Payment  │    │ Booking  │
│          │    │ (Redis)  │    │  (Kafka) │    │ (Kafka)  │    │ (Kafka)  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘

COMPENSATION (Payment Failed):
┌──────────┐    ┌──────────┐    ┌──────────┐
│ Payment  │───▶│ Release  │───▶│  Notify  │
│  Failed  │    │   Seat   │    │   User   │
│ (Kafka)  │    │ (Redis)  │    │ (Kafka)  │
└──────────┘    └──────────┘    └──────────┘

COMPENSATION (Timeout):
┌──────────┐    ┌──────────┐    ┌──────────┐
│   TTL    │───▶│ Release  │───▶│  Cancel  │
│ Expired  │    │   Seat   │    │ Booking  │
│ (Redis)  │    │ (Auto)   │    │          │
└──────────┘    └──────────┘    └──────────┘
```

---

## Corner Cases & Mitigations

### 1. Duplicate Booking Requests
**Scenario**: User clicks "Book" multiple times rapidly.
**Mitigation**: 
- Idempotency key in request headers
- Redis lock prevents duplicate processing
- Database unique constraint on (event_id, seat_id, status=CONFIRMED)

### 2. Double-Click Seat Booking
**Scenario**: Two requests try to lock same seat simultaneously.
**Mitigation**:
- Redis SETNX for atomic seat lock
- First request wins, second gets "Seat already locked" error
- Lock includes TTL to prevent deadlocks

### 3. Expired JWT
**Scenario**: Token expires mid-session.
**Mitigation**:
- API Gateway validates token expiry
- Short-lived access tokens (15 min)
- Refresh tokens for seamless renewal
- 401 response triggers client-side refresh

### 4. Invalid Tenant Subdomain
**Scenario**: Request to non-existent tenant.
**Mitigation**:
- TenantFilter validates against tenant registry (cached in Redis)
- Returns 404 "Tenant not found" for invalid subdomains
- No database operations attempted

### 5. Tenant Disabled
**Scenario**: Admin suspends a tenant during active usage.
**Mitigation**:
- TenantFilter checks tenant.status == ACTIVE
- Returns 403 "Tenant suspended" for non-active tenants
- Cache invalidation on tenant status change

### 6. Seat Already Locked
**Scenario**: User selects seat that another user is holding.
**Mitigation**:
- Real-time availability API checks Redis
- UI shows seat as "held" (yellow)
- Clear error message with suggestion to select another seat

### 7. Payment Arrives After Lock Expired
**Scenario**: Slow payment processing, seat lock TTL expires.
**Mitigation**:
- Booking Service checks seat availability before confirming
- If seat taken, initiate refund via PaymentService
- Publish BookingFailed event with reason

### 8. Kafka Message Replay
**Scenario**: Consumer restarts and reprocesses messages.
**Mitigation**:
- Idempotent consumers using processed_events table
- Unique constraint on (event_id, consumer_group)
- Skip already-processed events

### 9. Service Crash Recovery
**Scenario**: Booking Service crashes mid-saga.
**Mitigation**:
- Saga state persisted in database
- Recovery job scans incomplete sagas on startup
- Compensating transactions for stale sagas

### 10. DB Connection Exhaustion
**Scenario**: Connection pool depleted under load.
**Mitigation**:
- HikariCP with bounded pool (max 20 per service)
- Circuit breaker trips after threshold
- Bulkhead isolates tenants (separate pools optional)

### 11. Schema Creation Race Condition
**Scenario**: Two requests try to create same tenant simultaneously.
**Mitigation**:
- Distributed lock using Redis before schema creation
- Database constraint on tenant.schema_name
- Idempotent schema creation (CREATE IF NOT EXISTS)

### 12. Distributed Partial Failure
**Scenario**: Booking confirmed but notification fails.
**Mitigation**:
- Eventually consistent notifications
- Retry with exponential backoff
- Dead Letter Queue for failed notifications
- Manual reconciliation dashboard

### 13. Event Ordering Guarantees
**Scenario**: PaymentCompleted arrives before BookingRequested.
**Mitigation**:
- Kafka partitioned by tenantId ensures ordering within partition
- Event timestamp validation
- Retry mechanism for out-of-order events

### 14. Booking Cancellation After Confirmation
**Scenario**: User cancels confirmed booking.
**Mitigation**:
- Check cancellation policy (time-based rules)
- Initiate refund saga
- Release seat only after refund processed
- Maintain audit trail

---

## Security Model

### JWT Structure
```json
{
  "sub": "user-uuid",
  "tenantId": "tenant_acme",
  "role": "CUSTOMER",
  "iat": 1735689600,
  "exp": 1735690500,
  "jti": "unique-token-id"
}
```

### Security Rules
1. **Tenant Isolation**: JWT tenantId MUST match subdomain tenant
2. **Cross-Tenant Prevention**: Database queries always include tenant context
3. **Role-Based Access**: ADMIN, ORGANIZER, CUSTOMER roles
4. **Rate Limiting**: Per-tenant, per-endpoint limits

---

## Database Schema (Per Tenant)

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Events table
CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255),
    event_date TIMESTAMP NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    price_per_seat DECIMAL(10,2),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Seats table
CREATE TABLE seats (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    row_number VARCHAR(10),
    seat_number VARCHAR(10),
    category VARCHAR(50),
    price DECIMAL(10,2),
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    UNIQUE(event_id, row_number, seat_number)
);

-- Bookings table
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    seat_id UUID NOT NULL REFERENCES seats(id),
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

-- Payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50),
    transaction_ref VARCHAR(255),
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Processed Events (Idempotency)
CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP DEFAULT NOW()
);

-- Saga State
CREATE TABLE saga_state (
    id UUID PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(255) UNIQUE NOT NULL,
    current_step VARCHAR(100),
    status VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_event ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_seats_event ON seats(event_id);
CREATE INDEX idx_seats_status ON seats(status);
CREATE INDEX idx_payments_booking ON payments(booking_id);
```

---

## Scaling Strategy

### Horizontal Scaling
- Stateless services behind load balancer
- Kafka consumer groups for parallel processing
- Redis cluster for distributed caching/locking
- PostgreSQL read replicas for queries

### Vertical Scaling
- Container resource limits (CPU/Memory)
- Connection pool tuning
- JVM heap optimization

### Per-Tenant Scaling
- Tenant-specific rate limits
- Premium tenants get dedicated resources
- Tenant activity monitoring for capacity planning

---

## Deployment Model

### Docker Compose (Development)
- All services in single compose file
- Internal network isolation
- Volume mounts for persistence

### Kubernetes (Production)
- Helm charts per service
- Horizontal Pod Autoscaler
- PodDisruptionBudget for availability
- ConfigMaps and Secrets for configuration
- Network Policies for isolation

---

## Consistency Model

- **Strong Consistency**: Within single service database operations
- **Eventual Consistency**: Cross-service via Kafka events
- **At-Least-Once Delivery**: Idempotent consumers handle duplicates
- **Saga Pattern**: Distributed transactions with compensation

---

## Future Evolution Path

1. **GraphQL Gateway**: Federated API for complex queries
2. **Event Sourcing**: Complete audit trail with event replay
3. **CQRS**: Separate read/write models for performance
4. **Multi-Region**: Geographic distribution with CockroachDB
5. **Real-Time**: WebSocket for live seat availability
6. **ML Integration**: Demand prediction, dynamic pricing
7. **Tenant Sharding**: Physical database separation for scale

---

## Performance Optimizations

1. **Connection Pooling**: HikariCP with optimal settings
2. **Query Optimization**: Indexed queries, explain analyze
3. **Caching Strategy**: Redis for hot data, local cache for static
4. **Async Processing**: Non-blocking I/O, virtual threads
5. **Batch Operations**: Bulk inserts, batch consumers
6. **Compression**: Kafka message compression, HTTP gzip
