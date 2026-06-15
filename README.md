# Acoustic Guard Hub

A distributed urban acoustic monitoring system backend that processes real-time audio data from edge sensors, detects
threats using machine learning, and provides spatial incident tracking with real-time WebSocket updates.

## Architecture Overview

The Acoustic Guard Hub is built on an event-driven architecture designed for high-throughput real-time data processing:

- **Event-Driven Messaging**: RabbitMQ acts as the central message broker, ingesting audio frames and telemetry events
  from distributed edge sensors
- **Spatial Data Handling**: PostgreSQL with PostGIS extension enables geospatial queries for incident correlation and
  bounding box filtering
- **Real-Time Updates**: WebSocket broadcasting pushes live telemetry, sensor status changes, and incident updates to
  connected frontend clients
- **Layered Architecture**: Clean separation between controllers (REST API), services (business logic), repositories (
  data access), and messaging consumers (event processing)

## Tech Stack

- **Java 21**: Modern Java with record types, pattern matching, and enhanced switch expressions
- **Spring Boot 3.3**: Framework for dependency injection, transaction management, and WebSocket support
- **PostgreSQL 15 with PostGIS 3.3**: Spatial database for storing sensor locations, incidents, and enabling geospatial
  queries
- **RabbitMQ 4.3**: Message broker for asynchronous audio frame and telemetry event processing
- **Testcontainers 1.19.8**: Integration testing with Docker containers for PostgreSQL and RabbitMQ
- **MapStruct 1.6**: Compile-time bean mapping for DTO/entity conversions
- **JUnit 5 & Mockito 5.8**: Enterprise-grade testing framework with BDD-style nested tests
- **gRPC**: Communication protocol for the threat classification microservice
- **Liquibase**: Database migration management

## Prerequisites

- **Docker Desktop**: Required for running PostgreSQL, PostGIS, and RabbitMQ containers
- **JDK 21**: Java Development Kit version 21 or higher
- **Gradle 9.4**: Build tool (wrapper included in project)

## Local Setup & Running

### 1. Start Infrastructure Services

Start the required Docker containers for PostgreSQL, PostGIS, and RabbitMQ:

```bash
docker-compose up -d
```

This will start:

- PostgreSQL 15 with PostGIS 3.3 extension on port 5432
- RabbitMQ 4.3 with management UI on port 5672 (AMQP) and 15672 (management)

### 2. Configure Application

The application uses environment variables or `application.yml` for configuration. Key configuration properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/acoustic_guard
    username: acoustic_guard
    password: acoustic_guard
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

acoustic:
  classifier:
    confidence-threshold: 0.10
  sensor:
    heartbeat-timeout-seconds: 60
  incident:
    spatial-threshold-meters: 500
    temporal-threshold-seconds: 300
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 4. Access API Documentation

OpenAPI/Swagger documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

## Running Tests

The project uses Testcontainers for integration testing, which requires Docker to be running.

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Classes

```bash
./gradlew test --tests "*IncidentServiceImplTest"
./gradlew test --tests "*TelemetryServiceImplTest"
./gradlew test --tests "*SensorMonitorServiceImplTest"
```

### Run Integration Tests

Integration tests require Docker Desktop with proper daemon configuration for Testcontainers:

```bash
./gradlew test --tests "*IntegrationTest"
```

**Note**: Integration tests are currently disabled due to Docker daemon configuration requirements. Remove `@Disabled`
annotations from integration test classes to enable them once Docker is properly configured.

## Core Workflows

### Audio Frame Processing Workflow

```
Edge Sensor → RabbitMQ (q.frames) → AudioFrameConsumer 
→ AudioFrameService → ClassifierGrpcClient (gRPC) 
→ AlertService (if confidence threshold met) 
→ AlertRepository → WebSocket (/topic/alerts)
```

1. Edge sensors capture audio and publish `AudioFrame` messages to RabbitMQ
2. `AudioFrameConsumer` receives messages and delegates to `AudioFrameService`
3. `AudioFrameService` calls the classification microservice via gRPC
4. If classification confidence exceeds threshold (default 0.10), an alert is created
5. Alert is persisted to PostgreSQL and broadcast via WebSocket

### Telemetry Processing Workflow

```
Edge Sensor → RabbitMQ (q.telemetry) → TelemetryConsumer 
→ TelemetryService → In-Memory NodeState Map 
→ SensorPersistenceService (throttled DB writes) 
→ WebSocket (/topic/telemetry)
```

1. Edge sensors publish telemetry events to RabbitMQ
2. `TelemetryConsumer` receives messages and updates in-memory telemetry state
3. dBFS values are converted to dB SPL for frontend display
4. Database persistence is throttled (5-minute intervals) to prevent write amplification
5. Real-time telemetry is broadcast via WebSocket every 2 seconds

### Incident Correlation Workflow

```
AlertService.createAlert() → IncidentService.aggregateAlerts() 
→ Spatial Correlation (PostGIS ST_DWithin) 
→ Temporal Correlation (300-second window) 
→ IncidentRepository → WebSocket (/topic/incidents)
```

1. New alerts trigger incident aggregation logic
2. Alerts within 500 meters and 300 seconds are correlated into incidents
3. PostGIS spatial queries enable efficient geospatial correlation
4. Incidents are persisted and broadcast via WebSocket

### Sensor Health Monitoring Workflow

```
@Scheduled (every 5 seconds) → SensorHealthMonitorService.checkOfflineSensors() 
→ TelemetryService.getSensorStatus() 
→ Status Transition (ONLINE ↔ OFFLINE) 
→ SensorRepository → WebSocket (/topic/sensors)
```

1. Background job runs every 5 seconds to check sensor health
2. Sensors without recent heartbeats (60-second timeout) are marked OFFLINE
3. Sensors that recover are marked ONLINE
4. Status changes are persisted and broadcast via WebSocket

## Project Structure

```
hub/
├── src/main/java/com/acousticguard/hub/
│   ├── alert/           # Alert management (CRUD, status updates)
│   ├── analytics/       # Analytics and reporting
│   ├── auth/            # Authentication and authorization
│   ├── classifier/      # gRPC client for threat classification
│   ├── common/          # Shared domain objects, enums, error handling
│   ├── incident/        # Incident aggregation and spatial queries
│   ├── messaging/       # RabbitMQ consumers and configuration
│   ├── sensor/          # Sensor registry and health monitoring
│   ├── telemetry/       # Real-time telemetry processing
│   └── websocket/       # WebSocket broadcasting
├── src/test/java/       # Unit and integration tests
└── src/main/resources/   # Configuration files
```

## API Endpoints

### Sensors

- `GET /api/v1/sensors` - Retrieve all sensors with status and latency
- `GET /api/v1/sensors/{id}` - Retrieve specific sensor details

### Incidents

- `GET /api/v1/incidents` - Retrieve active incidents (optional bbox filtering)
- `GET /api/v1/incidents/{id}` - Retrieve specific incident by UUID
- `PATCH /api/v1/incidents/{id}/status` - Update incident status

### Alerts

- `GET /api/v1/alerts` - Retrieve all alerts
- `GET /api/v1/alerts/{id}` - Retrieve specific alert
- `PATCH /api/v1/alerts/{id}` - Update alert details

### Analytics

- `GET /api/v1/analytics/threat-distribution` - Get threat type distribution
- `GET /api/v1/analytics/incident-history` - Get incident time series data

## Configuration Properties

| Property                                       | Default | Description                               |
|------------------------------------------------|---------|-------------------------------------------|
| `acoustic.classifier.confidence-threshold`     | 0.10    | Minimum confidence for creating alerts    |
| `acoustic.sensor.heartbeat-timeout-seconds`    | 60      | Seconds before sensor is marked offline   |
| `acoustic.incident.spatial-threshold-meters`   | 500     | Maximum distance for alert correlation    |
| `acoustic.incident.temporal-threshold-seconds` | 300     | Maximum time window for alert correlation |

## License

Proprietary - All rights reserved.
