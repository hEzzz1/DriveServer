package com.example.demo.stats;

import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatsModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertEventRepository alertEventRepository;

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

        Role viewer = saveRole("VIEWER", "观察员");
        UserAccount viewerUser = saveUser("viewer", "123456", 1);
        bindUserRole(viewerUser.getId(), viewer.getId());
    }

    @Test
    void trendEndpointShouldAggregateByHour() throws Exception {
        saveAlert("ALT-TREND-001", 1001L, 2001L, 3001L, 11L, 3, "0.90", "0.80", "0.70", 0, "2026-04-20T10:15:00Z");
        saveAlert("ALT-TREND-002", 1001L, 2002L, 3002L, 11L, 2, "0.60", "0.50", "0.40", 1, "2026-04-20T10:45:00Z");
        saveAlert("ALT-TREND-003", 1001L, 2001L, 3001L, 12L, 3, "0.80", "0.70", "0.60", 3, "2026-04-20T11:05:00Z");

        String token = loginAndGetToken("viewer", "123456");

        mockMvc.perform(get("/api/v1/stats/trend")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("fleetId", "1001")
                        .queryParam("groupBy", "HOUR")
                        .queryParam("startTime", "2026-04-20T10:00:00Z")
                        .queryParam("endTime", "2026-04-20T11:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groupBy").value("HOUR"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].bucketTime").value("2026-04-20T10:00:00Z"))
                .andExpect(jsonPath("$.data.items[0].alertCount").value(2))
                .andExpect(jsonPath("$.data.items[0].highRiskCount").value(1))
                .andExpect(jsonPath("$.data.items[0].avgRiskScore").value(0.7500))
                .andExpect(jsonPath("$.data.items[1].bucketTime").value("2026-04-20T11:00:00Z"))
                .andExpect(jsonPath("$.data.items[1].alertCount").value(1))
                .andExpect(jsonPath("$.data.items[1].highRiskCount").value(1))
                .andExpect(jsonPath("$.data.items[1].avgFatigueScore").value(0.7000));
    }

    @Test
    void rankingEndpointShouldSortByAverageRiskScore() throws Exception {
        saveAlert("ALT-RANK-001", 1001L, 2001L, 3001L, 11L, 3, "0.90", "0.80", "0.70", 0, "2026-04-21T08:00:00Z");
        saveAlert("ALT-RANK-002", 1001L, 2002L, 3002L, 11L, 2, "0.50", "0.40", "0.30", 1, "2026-04-21T09:00:00Z");
        saveAlert("ALT-RANK-003", 1001L, 2003L, 3003L, 12L, 3, "0.95", "0.88", "0.82", 0, "2026-04-21T10:00:00Z");
        saveAlert("ALT-RANK-004", 1001L, 2001L, 3001L, 13L, 3, "0.80", "0.72", "0.61", 3, "2026-04-21T11:00:00Z");

        String token = loginAndGetToken("viewer", "123456");

        mockMvc.perform(get("/api/v1/stats/ranking")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("fleetId", "1001")
                        .queryParam("dimension", "DRIVER_ID")
                        .queryParam("sortBy", "AVG_RISK_SCORE")
                        .queryParam("limit", "2")
                        .queryParam("startTime", "2026-04-21T00:00:00Z")
                        .queryParam("endTime", "2026-04-21T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dimension").value("DRIVER_ID"))
                .andExpect(jsonPath("$.data.sortBy").value("AVG_RISK_SCORE"))
                .andExpect(jsonPath("$.data.totalDimensionCount").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].dimensionValue").value(3003))
                .andExpect(jsonPath("$.data.items[0].avgRiskScore").value(0.9500))
                .andExpect(jsonPath("$.data.items[1].rank").value(2))
                .andExpect(jsonPath("$.data.items[1].dimensionValue").value(3001))
                .andExpect(jsonPath("$.data.items[1].alertCount").value(2))
                .andExpect(jsonPath("$.data.items[1].highRiskCount").value(2))
                .andExpect(jsonPath("$.data.items[1].avgRiskScore").value(0.8500));
    }

    @Test
    void rankingEndpointShouldRejectInvalidEnumParam() throws Exception {
        String token = loginAndGetToken("viewer", "123456");

        mockMvc.perform(get("/api/v1/stats/ranking")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("dimension", "BAD_DIMENSION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求参数不合法"));
    }

    @Test
    void rankingEndpointShouldReturnEmptyItemsWhenNoData() throws Exception {
        String token = loginAndGetToken("viewer", "123456");

        mockMvc.perform(get("/api/v1/stats/ranking")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("dimension", "VEHICLE_ID")
                        .queryParam("startTime", "2026-04-01T00:00:00Z")
                        .queryParam("endTime", "2026-04-01T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalDimensionCount").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    private void saveAlert(String alertNo,
                           Long fleetId,
                           Long vehicleId,
                           Long driverId,
                           Long ruleId,
                           int riskLevel,
                           String riskScore,
                           String fatigueScore,
                           String distractionScore,
                           int status,
                           String triggerTime) {
        AlertEvent alert = new AlertEvent();
        LocalDateTime now = LocalDateTime.of(2026, 4, 25, 10, 0, 0);
        alert.setAlertNo(alertNo);
        alert.setFleetId(fleetId);
        alert.setVehicleId(vehicleId);
        alert.setDriverId(driverId);
        alert.setRuleId(ruleId);
        alert.setRiskLevel((byte) riskLevel);
        alert.setRiskScore(new BigDecimal(riskScore));
        alert.setFatigueScore(new BigDecimal(fatigueScore));
        alert.setDistractionScore(new BigDecimal(distractionScore));
        alert.setTriggerTime(OffsetDateTime.parse(triggerTime).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        alert.setStatus((byte) status);
        alert.setLatestActionBy(1L);
        alert.setLatestActionTime(now);
        alert.setRemark("seed");
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        alertEventRepository.save(alert);
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

    private String loginAndGetToken(String username, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("token").asText();
    }
}
