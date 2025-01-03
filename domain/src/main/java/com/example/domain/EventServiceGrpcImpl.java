package com.example.domain;

import com.example.grpc.*;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EventServiceGrpcImpl extends EventServiceGrpc.EventServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(EventServiceGrpcImpl.class);

    private final EventRepository repository;

    public EventServiceGrpcImpl(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void getEventById(GetEventRequest request, StreamObserver<GetEventResponse> responseObserver) {
        Long id = request.getId();
        log.debug("gRPC getEventById called with id={}", id);

        com.example.grpc.Event entity = toProto(repository.findById(id));
        if (entity == null) {
            entity = com.example.grpc.Event.newBuilder().build();
            log.debug("Event with id={} not found, returning empty gRPC Event", id);
        } else {
            log.debug("Event with id={} found, returning gRPC Event", id);
        }

        GetEventResponse resp = GetEventResponse.newBuilder()
                .setEvent(entity)
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllEvents(Empty request, StreamObserver<GetAllEventsResponse> responseObserver) {
        log.debug("gRPC getAllEvents called");

        List<com.example.grpc.Event> protoList = repository.findAll()
                .stream()
                .map(this::toProto)
                .collect(Collectors.toList());

        GetAllEventsResponse resp = GetAllEventsResponse.newBuilder()
                .addAllEvents(protoList)
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        log.debug("Returned total {} events via gRPC", protoList.size());
    }

    private com.example.grpc.Event toProto(com.example.domain.Event e) {
        if (e == null) return null;
        return com.example.grpc.Event.newBuilder()
                .setId(e.getId())
                .setName(e.getName())
                .setDate(e.getDate())
                .setLocation(e.getLocation())
                .build();
    }
}
