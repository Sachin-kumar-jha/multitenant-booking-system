# Testing Strategy Guide

## Overview

This document outlines the testing strategy for the Ticket Booking Platform, covering unit tests, integration tests, and end-to-end testing approaches.

## Test Pyramid

```
                    /\
                   /  \
                  / E2E \
                 /  Tests \
                /──────────\
               /            \
              / Integration  \
             /     Tests      \
            /──────────────────\
           /                    \
          /      Unit Tests      \
         /────────────────────────\
```

## Unit Testing

### Test Coverage Goals
- Service Layer: 80%+ coverage
- Repository Layer: 70%+ coverage (query methods)
- Entity Layer: Business logic methods only

### Unit Test Structure

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Name Unit Tests")
class ServiceTest {
    @Mock private Repository repository;
    @InjectMocks private Service service;
    
    @Test
    @DisplayName("Should do something specific")
    void shouldDoSomething() {
        // Given
        // When
        // Then
    }
}
```

### Key Testing Patterns

1. **Arrange-Act-Assert (AAA)**: Structure every test with clear setup, action, and verification phases.

2. **Mock External Dependencies**: Use `@Mock` for repositories, external services, and event publishers.

3. **Test Business Logic**: Focus on testing business rules, validations, and state transitions.

4. **Test Edge Cases**: Include tests for null values, empty collections, boundary conditions.

## Integration Testing

### Testcontainers Setup

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### Integration Test Scenarios

1. **API Endpoint Tests**
   - Request/Response validation
   - Authentication/Authorization
   - Error handling

2. **Database Integration**
   - Repository query verification
   - Transaction behavior
   - Multitenancy isolation

3. **Kafka Integration**
   - Event publishing verification
   - Consumer behavior
   - Idempotency

4. **Redis Integration**
   - Caching behavior
   - Lock acquisition/release
   - TTL verification

## Running Tests

### Unit Tests Only
```bash
mvn test -Dtest=*Test
```

### Integration Tests
```bash
mvn verify -Dtest=*IntegrationTest
```

### All Tests with Coverage
```bash
mvn verify -Pcoverage
```

### Specific Service
```bash
mvn test -pl booking-service
```

## Test Configuration

### application-test.properties
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.org.springframework=WARN
eureka.client.enabled=false
```

## Contract Testing (Optional)

For service-to-service communication, consider Spring Cloud Contract:

```java
@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    ids = "com.ticket:event-service:+:stubs:8083"
)
class BookingServiceContractTest {
    // Contract verification tests
}
```

## Performance Testing

For load testing, use tools like:
- **Gatling**: For HTTP endpoint load testing
- **JMeter**: For comprehensive performance testing

### Sample Gatling Scenario
```scala
scenario("Booking Flow")
  .exec(http("Create Booking")
    .post("/api/bookings")
    .body(jsonPayload)
    .check(status.is(202)))
```

## Test Data Management

### Test Fixtures
```java
public class TestFixtures {
    public static Event createTestEvent() {
        Event event = new Event();
        event.setName("Test Concert");
        event.setEventDate(LocalDateTime.now().plusDays(30));
        return event;
    }
}
```

### Database Cleanup
- Use `@Transactional` for automatic rollback
- Use `@DirtiesContext` sparingly
- Consider database cleaning strategies for integration tests

## Continuous Integration

### GitHub Actions Example
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: mvn verify
```

## Monitoring Test Quality

### JaCoCo Coverage Reports
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Mutation Testing (PIT)
For advanced test quality verification:
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```
