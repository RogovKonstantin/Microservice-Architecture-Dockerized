package com.example.domain;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Domain service...");

        HibernateUtil.getSessionFactory();

        Server server = NettyServerBuilder.forPort(8081)
                .addService(new EventServiceGrpcImpl(new EventRepository()))
                .build()
                .start();

        log.info("gRPC server started, listening on port 8081");

        RabbitConsumer rabbitConsumer = new RabbitConsumer(new EventRepository());
        Thread consumerThread = new Thread(rabbitConsumer, "RabbitConsumerThread");
        consumerThread.start();
        log.info("RabbitConsumer thread started");

        server.awaitTermination();
        log.info("gRPC server terminated");
    }
}