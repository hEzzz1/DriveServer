package com.example.demo.ingest.stream;

import com.example.demo.ingest.dto.IngestEventRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingest.stream.producer", havingValue = "noop")
public class NoopEventStreamPublisher implements EventStreamPublisher {

    @Override
    public void publish(IngestEventRequest request) {
        // no-op for local tests
    }
}
