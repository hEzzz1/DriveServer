package com.example.demo.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestModuleIntegrationTest {

    private static final String DEVICE_TOKEN = "test-device-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ingestShouldAcceptValidEvent() throws Exception {
        String eventId = "evt_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    void ingestShouldRejectInvalidScore() throws Exception {
        String eventId = "evt_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "fleetId": "fleet_01",
                                  "vehicleId": "veh_001",
                                  "driverId": "drv_001",
                                  "eventTime": "2026-04-07T10:01:15Z",
                                  "fatigueScore": 1.01,
                                  "distractionScore": 0.64
                                }
                                """.formatted(eventId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void ingestShouldRejectDuplicateEventId() throws Exception {
        String eventId = "evt_dup_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void ingestShouldRejectWhenDeviceTokenMissingOrInvalid() throws Exception {
        String eventId = "evt_bad_token_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId + "_2")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    private String validPayload(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "fleetId": "fleet_01",
                  "vehicleId": "veh_001",
                  "driverId": "drv_001",
                  "eventTime": "2026-04-07T10:01:15Z",
                  "fatigueScore": 0.82,
                  "distractionScore": 0.64,
                  "perclos": 0.41,
                  "blinkRate": 0.28,
                  "yawnCount": 2,
                  "headPose": "DOWN",
                  "algorithmVer": "v1.0.3"
                }
                """.formatted(eventId);
    }
}
