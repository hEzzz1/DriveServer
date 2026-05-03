package com.example.demo.ingest.stream;

import com.example.demo.ingest.dto.IngestEventRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        body.put("vehicleId", request.getVehicleId());
        if (StringUtils.hasText(request.getFleetId())) {
            body.put("fleetId", request.getFleetId());
        }
        if (StringUtils.hasText(request.getDriverId())) {
            body.put("driverId", request.getDriverId());
        }
        String eventTimeUtc = request.getEventTime().toInstant().toString();
        body.put("eventTime", eventTimeUtc);
        body.put("eventTimeUtc", eventTimeUtc);
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
        if (StringUtils.hasText(request.getRiskLevel())) {
            body.put("riskLevel", request.getRiskLevel());
        }
        if (StringUtils.hasText(request.getDominantRiskType())) {
            body.put("dominantRiskType", request.getDominantRiskType());
        }
        if (request.getTriggerReasons() != null && !request.getTriggerReasons().isEmpty()) {
            body.put("triggerReasons", String.join(",", request.getTriggerReasons()));
        }
        if (request.getWindowStartMs() != null) {
            body.put("windowStartMs", String.valueOf(request.getWindowStartMs()));
        }
        if (request.getWindowEndMs() != null) {
            body.put("windowEndMs", String.valueOf(request.getWindowEndMs()));
        }
        if (request.getCreatedAtMs() != null) {
            body.put("createdAtMs", String.valueOf(request.getCreatedAtMs()));
        }
        if (StringUtils.hasText(request.getEvidenceType())) {
            body.put("evidenceType", request.getEvidenceType());
        }
        if (StringUtils.hasText(request.getEvidenceUrl())) {
            body.put("evidenceUrl", request.getEvidenceUrl());
        }
        if (StringUtils.hasText(request.getEvidenceMimeType())) {
            body.put("evidenceMimeType", request.getEvidenceMimeType());
        }
        if (request.getEvidenceCapturedAtMs() != null) {
            body.put("evidenceCapturedAtMs", String.valueOf(request.getEvidenceCapturedAtMs()));
        }

        RecordId recordId = stringRedisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(body).withStreamKey(streamKey));
        if (recordId == null) {
            throw new IllegalStateException("failed to publish event to redis stream");
        }
    }
}
