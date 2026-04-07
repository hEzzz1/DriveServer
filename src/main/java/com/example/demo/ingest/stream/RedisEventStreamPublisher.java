package com.example.demo.ingest.stream;

import com.example.demo.ingest.dto.IngestEventRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ingest.stream.producer", havingValue = "redis", matchIfMissing = true)
public class RedisEventStreamPublisher implements EventStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final String streamKey;

    public RedisEventStreamPublisher(StringRedisTemplate stringRedisTemplate,
                                     @Value("${ingest.stream.key:stream:events}") String streamKey) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.streamKey = streamKey;
    }

    @Override
    public void publish(IngestEventRequest request) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("eventId", request.getEventId());
        body.put("fleetId", request.getFleetId());
        body.put("vehicleId", request.getVehicleId());
        body.put("driverId", request.getDriverId());
        body.put("eventTime", request.getEventTime().toInstant().toString());
        body.put("fatigueScore", request.getFatigueScore().toPlainString());
        body.put("distractionScore", request.getDistractionScore().toPlainString());
        if (request.getPerclos() != null) {
            body.put("perclos", request.getPerclos().toPlainString());
        }
        if (request.getBlinkRate() != null) {
            body.put("blinkRate", request.getBlinkRate().toPlainString());
        }
        if (request.getYawnCount() != null) {
            body.put("yawnCount", String.valueOf(request.getYawnCount()));
        }
        if (request.getHeadPose() != null) {
            body.put("headPose", request.getHeadPose());
        }
        if (request.getAlgorithmVer() != null) {
            body.put("algorithmVer", request.getAlgorithmVer());
        }

        RecordId recordId = stringRedisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(body).withStreamKey(streamKey));
        if (recordId == null) {
            throw new IllegalStateException("failed to publish event to redis stream");
        }
    }
}
