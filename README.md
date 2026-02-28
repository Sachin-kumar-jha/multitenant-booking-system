# Multitenant Event-Driven Ticket Booking SaaS Platform

A production-grade, enterprise-level microservices platform for ticket booking built with Java 21 and Spring Boot 3.x.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client Applications                            │
│                    (Web, Mobile, Third-party APIs)                       │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            API Gateway                                   │
│            (Rate Limiting, JWT Validation, Routing)                      │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
    ┌───────────┐         ┌─────────────┐         ┌─────────────┐
    │   Auth    │         │    Event    │         │   Booking   │
    │  Service  │         │   Service   │         │   Service   │
    └───────────┘         └─────────────┘         └─────────────┘
          │                       │                       │
          └───────────────────────┴───────────────────────┘
                                  │
                      ┌───────────┴───────────┐
                      ▼                       ▼
               ┌─────────────┐         ┌─────────────┐
               │   Payment   │         │ Notification │
               │   Service   │         │   Service    │
               └─────────────┘         └─────────────┘
```

## Features

- **Multitenancy**: Schema-per-tenant isolation with automatic provisioning
- **Event-Driven Architecture**: Kafka-based async communication
- **Saga Pattern**: Distributed transactions for booking flow
- **Seat Locking**: Redis-based TTL locks for concurrent access
- **JWT Authentication**: Stateless auth with tenant context
- **Rate Limiting**: Per-tenant and per-user request throttling
- **Circuit Breaker**: Resilience4j fault tolerance

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Message Broker | Apache Kafka (KRaft) |
| Database | PostgreSQL 15 |
| Cache/Locks | Redis 7 |
| Container | Docker |

## Services

| Service | Port | Description |
|---------|------|-------------|
| Service Registry | 8761 | Eureka service discovery |
| API Gateway | 8080 | Entry point, routing, auth |
| Tenant Service | 8081 | Tenant management |
| Auth Service | 8082 | Authentication |
| Event Service | 8083 | Event & seat management |
| Booking Service | 8084 | Booking orchestration |
| Payment Service | 8085 | Payment processing |
| Notification Service | 8086 | Email/SMS notifications |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development)
- Maven 3.9+

### Running with Docker

```bash
# Start infrastructure
docker-compose up -d postgres redis kafka

# Build all services
mvn clean package -DskipTests

# Start all services
docker-compose up -d
```

### Running Locally

1. Start infrastructure:
```bash
docker-compose up -d postgres redis kafka
```

2. Build the project:
```bash
mvn clean install
```

3. Start services in order:
```bash
cd service-registry && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd tenant-service && mvn spring-boot:run &
cd auth-service && mvn spring-boot:run &
cd event-service && mvn spring-boot:run &
cd booking-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
```

## API Usage

### 1. Register a Tenant

```bash
curl -X POST http://localhost:8080/api/tenants/register \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-corp",
    "name": "ACME Corporation",
    "adminEmail": "admin@acme.com",
    "subscriptionPlan": "PROFESSIONAL"
  }'
```

### 2. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: acme-corp" \
  -d '{
    "email": "user@acme.com",
    "password": "securePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: acme-corp" \
  -d '{
    "email": "user@acme.com",
    "password": "securePassword123"
  }'
```

### 4. Create an Event (Admin)

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "Summer Music Festival",
    "description": "Annual outdoor concert",
    "venue": "Central Park Arena",
    "eventDate": "2024-08-15T19:00:00",
    "category": "CONCERT",
    "totalSeats": 1000,
    "basePrice": 75.00
  }'
```

### 5. Book Tickets

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "eventId": 1,
    "seatIds": [1, 2, 3],
    "paymentMethod": "CREDIT_CARD"
  }'
```

## Configuration

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ticket_system
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET_KEY=YourVeryLongSecretKey...
JWT_EXPIRATION=900000
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn verify -Pcoverage

# Run integration tests (requires Docker)
mvn verify -Pit
```

## Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8090
- **Actuator Endpoints**: http://localhost:{port}/actuator

## Documentation

- [Architecture Guide](ARCHITECTURE.md) - Detailed system design
- [Testing Guide](TESTING.md) - Testing strategy and patterns

## Project Structure

```
ticket-booking/
├── common-lib/           # Shared library
├── service-registry/     # Eureka server
├── api-gateway/          # Spring Cloud Gateway
├── tenant-service/       # Tenant management
├── auth-service/         # Authentication
├── event-service/        # Event management
├── booking-service/      # Booking orchestration
├── payment-service/      # Payment processing
├── notification-service/ # Notifications
├── docker-compose.yml    # Container orchestration
└── pom.xml              # Parent POM
```

## License

MIT License - see LICENSE file for details.
