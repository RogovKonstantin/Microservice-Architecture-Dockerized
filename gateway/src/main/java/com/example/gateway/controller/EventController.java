package com.example.gateway.controller;

import com.example.gateway.service.EventCacheService;
import com.example.grpc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.StatusRuntimeException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/events")
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventServiceGrpc.EventServiceBlockingStub eventServiceBlockingStub;
    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;
    private final EventCacheService eventCacheService;

    @Value("${spring.rabbitmq.template.default-receive-queue}")
    private String queueName;

    public EventController(EventServiceGrpc.EventServiceBlockingStub eventServiceBlockingStub,
                           AmqpTemplate amqpTemplate,
                           ObjectMapper objectMapper,
                           EventCacheService eventCacheService) {
        this.eventServiceBlockingStub = eventServiceBlockingStub;
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
        this.eventCacheService = eventCacheService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        log.info("Request to GET event by id={}", id);

        Map<String, Object> cached = eventCacheService.getCachedEvent(id);
        if (cached != null) {
            log.debug("Event id={} found in Redis cache", id);
            return ResponseEntity.ok(cached);
        }

        try {
            GetEventRequest req = GetEventRequest.newBuilder().setId(id).build();
            GetEventResponse resp = eventServiceBlockingStub.getEventById(req);
            com.example.grpc.Event event = resp.getEvent();

            if (event == null || event.getId() == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "Event not found"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", event.getId());
            result.put("name", event.getName());
            result.put("date", event.getDate());
            result.put("location", event.getLocation());

            eventCacheService.cacheEvent(event.getId(), result);
            log.info("Event id={} fetched via gRPC and cached", id);
            return ResponseEntity.ok(result);
        } catch (StatusRuntimeException ex) {
            log.error("Error fetching event id={} via gRPC: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Internal server error"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllEvents() {
        log.info("Request to GET all events");

        try {
            GetAllEventsResponse resp = eventServiceBlockingStub.getAllEvents(Empty.newBuilder().build());
            List<Map<String, Object>> list = new ArrayList<>();
            for (com.example.grpc.Event event : resp.getEventsList()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", event.getId());
                map.put("name", event.getName());
                map.put("date", event.getDate());
                map.put("location", event.getLocation());
                list.add(map);
            }
            log.info("Total events fetched: {}", list.size());
            return ResponseEntity.ok(list);
        } catch (StatusRuntimeException ex) {
            log.error("Error fetching all events via gRPC: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> eventData) {
        log.info("Request to CREATE event: {}", eventData);
        try {
            sendAsyncOperation("CREATE", eventData);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Collections.singletonMap("message", "Event creation in progress"));
        } catch (Exception e) {
            log.error("Error creating event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to create event"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> eventData) {
        log.info("Request to UPDATE event id={}, data={}", id, eventData);
        eventData.put("id", id);
        try {
            sendAsyncOperation("UPDATE", eventData);

            eventCacheService.updateCachedEvent(id, eventData);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Collections.singletonMap("message", "Event update in progress and cache updated"));
        } catch (Exception e) {
            log.error("Error updating event id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to update event"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        log.info("Request to DELETE event id={}", id);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", id);
        try {
            sendAsyncOperation("DELETE", eventData);

            eventCacheService.deleteCachedEvent(id);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Collections.singletonMap("message", "Event deletion in progress and cache cleared"));
        } catch (Exception e) {
            log.error("Error deleting event id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to delete event"));
        }
    }


    private void sendAsyncOperation(String operation, Map<String, Object> eventData) throws Exception {
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("operation", operation);
        messageBody.put("event", eventData);
        messageBody.put("requestId", requestId);

        String jsonMsg = objectMapper.writeValueAsString(messageBody);
        amqpTemplate.convertAndSend(queueName, jsonMsg);
        log.debug("Message sent to RabbitMQ queue={}, operation={}, requestId={}", queueName, operation, requestId);
    }
}