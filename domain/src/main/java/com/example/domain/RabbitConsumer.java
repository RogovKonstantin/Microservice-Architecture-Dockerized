package com.example.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RabbitConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumer.class);

    private static final String QUEUE_NAME = "event-queue";
    private static final String RABBIT_HOST = "rabbitmq";
    private static final int RABBIT_PORT = 5672;
    private static final String RABBIT_USER = "guest";
    private static final String RABBIT_PASS = "guest";

    private final EventRepository repository;
    private final ObjectMapper mapper;

    public RabbitConsumer(EventRepository repository) {
        this.repository = repository;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void run() {
        log.info("RabbitConsumer is starting...");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBIT_HOST);
        factory.setPort(RABBIT_PORT);
        factory.setUsername(RABBIT_USER);
        factory.setPassword(RABBIT_PASS);

        int maxRetries = 10;
        int retryInterval = 5000;

        while (maxRetries > 0) {
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                log.info("RabbitConsumer waiting for messages on queue '{}'", QUEUE_NAME);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info("Received message: {}", msg);
                    handleMessage(msg);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };
                CancelCallback cancelCallback = consumerTag -> {
                    log.warn("Consumer cancelled: {}", consumerTag);
                };

                channel.basicConsume(QUEUE_NAME, false, deliverCallback, cancelCallback);


                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
                return;

            } catch (IOException | TimeoutException | InterruptedException e) {
                maxRetries--;
                log.error("Failed to connect to RabbitMQ. Retries left: {}. Retrying in {}ms...", maxRetries, retryInterval, e);
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("RabbitConsumer failed to connect to RabbitMQ after multiple retries. Shutting down.");
    }

    private void handleMessage(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String operation = root.get("operation").asText();
            JsonNode eventNode = root.get("event");

            log.debug("Handling message with operation={}", operation);

            switch (operation) {
                case "CREATE" -> {
                    Event event = new Event();
                    event.setName(eventNode.get("name").asText());
                    event.setDate(eventNode.get("date").asText());
                    event.setLocation(eventNode.get("location").asText());
                    repository.save(event);
                    log.info("Created event with ID={}", event.getId());
                }
                case "UPDATE" -> {
                    Long id = eventNode.get("id").asLong();
                    Event existing = repository.findById(id);
                    if (existing != null) {
                        existing.setName(eventNode.get("name").asText());
                        existing.setDate(eventNode.get("date").asText());
                        existing.setLocation(eventNode.get("location").asText());
                        repository.update(existing);
                        log.info("Updated event with ID={}", id);
                    } else {
                        log.warn("Event with ID={} not found for update", id);
                    }
                }
                case "DELETE" -> {
                    Long id = eventNode.get("id").asLong();
                    repository.delete(id);
                    log.info("Deleted event with ID={}", id);
                }
                default -> {
                    log.warn("Unknown operation: {}", operation);
                }
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }
}
