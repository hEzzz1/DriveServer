package com.example.demo.ingest;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.device.entity.Device;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.rule.entity.RuleConfig;
import com.example.demo.rule.repository.RuleConfigRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestModuleIntegrationTest {

    private static final String DEVICE_CODE = "edge-device-01";
    private static final String DEVICE_TOKEN = "test-device-token";
    private static final long ENTERPRISE_ID = 1001L;
    private static final long FLEET_ID = 1002L;
    private static final long VEHICLE_ID = 1L;
    private static final long DRIVER_ID = 2001L;

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

    @Autowired
    private RuleConfigRepository ruleConfigRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        alertEventRepository.deleteAll();
        ruleConfigRepository.deleteAll();
        deviceRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role admin = saveRole("SUPER_ADMIN", "超级管理员");
        UserAccount adminUser = saveUser("admin", "123456", 1);
        saveSystemUser("system-auto-alert");
        saveDevice();
        bindUserRole(adminUser.getId(), admin.getId());
        saveRule("RISK_HIGH", "高风险规则", 3, "0.8000", 3, 60, true, "ENABLED");
        saveRule("RISK_MID", "中风险规则", 2, "0.6500", 5, 60, true, "ENABLED");
        saveRule("RISK_LOW", "低风险规则", 1, "0.5000", 8, 60, true, "ENABLED");
    }

    @Test
    void ingestShouldAcceptValidEvent() throws Exception {
        String eventId = "evt_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    void ingestShouldCreateWarningRecordImmediately() throws Exception {
        String eventId = "evt_warning_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithScores(eventId, "2026-04-07T10:01:15Z", 0.82, 0.64)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", bearerToken())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].vehicleId").value(VEHICLE_ID))
                .andExpect(jsonPath("$.data.items[0].riskLevel").value(3))
                .andExpect(jsonPath("$.data.items[0].fatigueScore").value(0.82));
    }

    @Test
    void ingestShouldPersistEdgeMetadata() throws Exception {
        String eventId = "evt_alias_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "fleetId": "%d",
                                  "vehicleId": "%d",
                                  "driverId": "%d",
                                  "eventTime": "2026-04-07T10:01:15Z",
                                  "fatigueScore": 0.72,
                                  "distractionScore": 0.68,
                                  "riskLevel": "HIGH",
                                  "dominantRiskType": "FATIGUE",
                                  "triggerReasons": ["EYE_CLOSED", "YAWN"],
                                  "windowStartMs": 1744010472000,
                                  "windowEndMs": 1744010475000,
                                  "createdAtMs": 1744010475200
                                }
                                """.formatted(eventId, FLEET_ID, VEHICLE_ID, DRIVER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accepted").value(true));

        List<AlertEvent> alerts = alertEventRepository.findAll();
        assertEquals(1, alerts.size());

        AlertEvent alert = alerts.get(0);
        Long highRuleId = ruleConfigRepository.findByRuleCode("RISK_HIGH")
                .orElseThrow()
                .getId();
        assertEquals(highRuleId, alert.getRuleId());
        assertEquals(Byte.valueOf((byte) 3), alert.getRiskLevel());
        assertEquals(0, alert.getRiskScore().compareTo(new BigDecimal("0.7200")));
        assertEquals("HIGH", alert.getEdgeRiskLevel());
        assertEquals("FATIGUE", alert.getEdgeDominantRiskType());
        assertEquals("EYE_CLOSED,YAWN", alert.getEdgeTriggerReasons());
        assertEquals(Long.valueOf(1744010472000L), alert.getEdgeWindowStartMs());
        assertEquals(Long.valueOf(1744010475000L), alert.getEdgeWindowEndMs());
        assertEquals(Long.valueOf(1744010475200L), alert.getEdgeCreatedAtMs());
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void ingestShouldRejectInvalidScore() throws Exception {
        String eventId = "evt_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "fleetId": "%d",
                                  "vehicleId": "%d",
                                  "driverId": "%d",
                                  "eventTime": "2026-04-07T10:01:15Z",
                                  "fatigueScore": 1.01,
                                  "distractionScore": 0.64
                                }
                                """.formatted(eventId, FLEET_ID, VEHICLE_ID, DRIVER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void ingestShouldRejectDuplicateEventId() throws Exception {
        String eventId = "evt_dup_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
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
                        .header("X-Device-Code", DEVICE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId + "_2")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void ingestShouldNotCreateAlertWhenNoRuleEnabled() throws Exception {
        ruleConfigRepository.deleteAll();
        String eventId = "evt_no_rules_" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Device-Code", DEVICE_CODE)
                        .header("X-Device-Token", DEVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accepted").value(true));

        assertEquals(0, alertEventRepository.count());
    }

    private String validPayload(String eventId) {
        return payloadWithScores(eventId, "2026-04-07T10:01:15Z", 0.82, 0.64);
    }

    private String payloadWithScores(String eventId, String eventTime, double fatigueScore, double distractionScore) {
        return """
                {
                  "eventId": "%s",
                  "fleetId": "%d",
                  "vehicleId": "%d",
                  "driverId": "%d",
                  "eventTime": "%s",
                  "fatigueScore": %.2f,
                  "distractionScore": %.2f,
                  "perclos": 0.41,
                  "blinkRate": 0.28,
                  "yawnCount": 2,
                  "headPose": "DOWN",
                  "algorithmVer": "v1.0.3"
                }
                """.formatted(eventId, FLEET_ID, VEHICLE_ID, DRIVER_ID, eventTime, fatigueScore, distractionScore);
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

    private Device saveDevice() {
        Device device = new Device();
        device.setEnterpriseId(ENTERPRISE_ID);
        device.setFleetId(FLEET_ID);
        device.setVehicleId(VEHICLE_ID);
        device.setDeviceCode(DEVICE_CODE);
        device.setDeviceName("边缘设备-01");
        device.setDeviceToken(DEVICE_TOKEN);
        device.setStatus(EdgeDeviceStatus.BOUND.name());
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    private UserAccount saveUser(String username, String password, int status) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setSubjectType(SubjectType.USER.name());
        user.setStatus((byte) status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userAccountRepository.save(user);
    }

    private UserAccount saveSystemUser(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("system-only"));
        user.setNickname(username);
        user.setSubjectType(SubjectType.SYSTEM.name());
        user.setStatus((byte) 0);
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

    private RuleConfig saveRule(String ruleCode,
                                String ruleName,
                                int riskLevel,
                                String riskThreshold,
                                int durationSeconds,
                                int cooldownSeconds,
                                boolean enabled,
                                String status) {
        RuleConfig rule = new RuleConfig();
        LocalDateTime now = LocalDateTime.now();
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleName);
        rule.setRiskLevel(riskLevel);
        rule.setRiskThreshold(new BigDecimal(riskThreshold));
        rule.setDurationSeconds(durationSeconds);
        rule.setCooldownSeconds(cooldownSeconds);
        rule.setEnabled(enabled);
        rule.setStatus(status);
        rule.setVersion(1);
        rule.setPublishedAt(enabled ? now : null);
        rule.setPublishedBy(enabled ? 1L : null);
        rule.setCreatedBy(1L);
        rule.setUpdatedBy(1L);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        return ruleConfigRepository.save(rule);
    }
}
