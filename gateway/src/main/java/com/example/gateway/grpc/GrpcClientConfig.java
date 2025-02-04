package com.example.gateway.grpc;

import com.example.grpc.EventServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${domain.grpc.host}")
    private String domainHost;

    @Value("${domain.grpc.port}")
    private int domainPort;

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder
                .forAddress(domainHost, domainPort)
                .usePlaintext()
                .build();
    }

    @Bean
    public EventServiceGrpc.EventServiceBlockingStub eventServiceBlockingStub(ManagedChannel channel) {
        return EventServiceGrpc.newBlockingStub(channel);
    }
}
