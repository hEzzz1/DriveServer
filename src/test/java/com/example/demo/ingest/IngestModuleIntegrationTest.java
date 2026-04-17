package com.example.demo.ingest;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.alert.repository.AlertEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestModuleIntegrationTest {

    private static final String DEVICE_TOKEN = "test-device-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        alertEventRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role admin = saveRole("ADMIN", "系统管理员");
        UserAccount adminUser = saveUser("admin", "123456", 1);
        bindUserRole(adminUser.getId(), admin.getId());
    }

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
    void ingestShouldAutoCreateAlertAfterRuleDurationReached() throws Exception {
        String eventIdBase = "evt_auto_alert_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithScores(eventIdBase + "_1", "2026-04-07T10:01:15Z", 0.92, 0.90)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithScores(eventIdBase + "_2", "2026-04-07T10:01:17Z", 0.92, 0.90)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithScores(eventIdBase + "_3", "2026-04-07T10:01:18Z", 0.92, 0.90)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", bearerToken())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].vehicleId").value(1))
                .andExpect(jsonPath("$.data.items[0].riskLevel").value(3))
                .andExpect(jsonPath("$.data.items[0].fatigueScore").value(0.92));
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
        return payloadWithScores(eventId, "2026-04-07T10:01:15Z", 0.82, 0.64);
    }

    private String payloadWithScores(String eventId, String eventTime, double fatigueScore, double distractionScore) {
        return """
                {
                  "eventId": "%s",
                  "fleetId": "fleet_01",
                  "vehicleId": "veh_001",
                  "driverId": "drv_001",
                  "eventTime": "%s",
                  "fatigueScore": %.2f,
                  "distractionScore": %.2f,
                  "perclos": 0.41,
                  "blinkRate": 0.28,
                  "yawnCount": 2,
                  "headPose": "DOWN",
                  "algorithmVer": "v1.0.3"
                }
                """.formatted(eventId, eventTime, fatigueScore, distractionScore);
    }

    private String bearerToken() {
        return "Bearer " + obtainToken();
    }

    private String obtainToken() {
        try {
            String response = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "admin",
                                      "password": "123456"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            JsonNode root = objectMapper.readTree(response);
            return root.path("data").path("token").asText();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Role saveRole(String roleCode, String roleName) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    private UserAccount saveUser(String username, String password, int status) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setStatus((byte) status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userAccountRepository.save(user);
    }

    private void bindUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }
}
