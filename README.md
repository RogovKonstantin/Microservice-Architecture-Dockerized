# Microservice Architecture with Docker Compose

This repository demonstrates a microservice-based architecture using Docker Compose. The architecture consists of several core components to ensure scalability, maintainability, and observability. The system includes gateway routing, CRUD operations, caching, logging, monitoring, and database storage.

# Architecture Components

## 1. Gateway Service
* Provides a RESTful API for external requests.
* Routes requests to the Domain service via gRPC.
* Implements:
  * Logging: Logs incoming and outgoing requests using ELK Stack.
  * Caching: Uses Redis for caching GET requests.
  * Metrics: Collects performance metrics via Prometheus and visualizes them in Grafana.

## 2. Domain Service
* Handles CRUD operations exposed via gRPC.
* Processes:
  * GET requests: Handled synchronously.
  * PUT, POST, DELETE requests: Sent asynchronously via RabbitMQ.
* Interacts with the database (PostgreSQL/MongoDB) for data persistence.

## 3. Redis
* Configured as a caching layer to speed up GET requests.

## 4. RabbitMQ
* Configured to handle message queuing for asynchronous operations between Gateway and Domain services.

## 5. ELK Stack
* OpenSearch: Indexes logs from Gateway and other services.
* Logstash: Collects and processes logs.
* Kibana: Visualizes logs and provides tools for event analysis.

## 6. Prometheus + Grafana
* Prometheus collects system metrics from services.
* Grafana visualizes metrics with custom dashboards.

## 7. Database
* Configured for CRUD operations by the Domain service.
* Includes an admin interface for managing the database:
  * PostgreSQL: Uses pgAdmin.

# Setup and Run

* Clone the repository: `git clone https://github.com/yourusername/Microservice-Architecture-Dockerized.git`
* Navigate into the directory: `cd Microservice-Architecture-Dockerized`
* Start the services: `docker-compose up --build`

# Access Components

* Gateway: `http://localhost:<gateway-port>`
* Kibana (Logs): `http://localhost:5601`
* Grafana (Metrics): `http://localhost:3000`
* pgAdmin: `http://localhost:5050`
