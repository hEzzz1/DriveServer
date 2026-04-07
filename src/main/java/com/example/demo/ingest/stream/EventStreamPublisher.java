package com.example.demo.ingest.stream;

import com.example.demo.ingest.dto.IngestEventRequest;

public interface EventStreamPublisher {
    void publish(IngestEventRequest request);
}
