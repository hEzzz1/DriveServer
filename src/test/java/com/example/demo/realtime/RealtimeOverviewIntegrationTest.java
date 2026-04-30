package com.example.demo.realtime;

import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
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
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RealtimeOverviewIntegrationTest {

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

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private FleetRepository fleetRepository;

    private Long enterpriseId;
    private Long fleetId;

    @BeforeEach
    void setUp() {
        alertEventRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        fleetRepository.deleteAll();
        enterpriseRepository.deleteAll();

        enterpriseId = saveEnterprise("ENT-REALTIME", "实时企业", 1).getId();
        fleetId = saveFleet(enterpriseId, "实时车队", 1).getId();
        Role viewer = saveRole("VIEWER", "观察员");
        UserAccount viewerUser = saveUser("viewer", "123456", 1, enterpriseId);
        bindUserRole(viewerUser.getId(), viewer.getId());
    }

    @Test
    void overviewShouldReturnWindowStatsLatestAlertsAndRiskDistribution() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        saveAlert("ALT-OVERVIEW-001", fleetId, 2001L, 3001L, 11L, 3, "0.91", 0, now.minusMinutes(2));
        saveAlert("ALT-OVERVIEW-002", fleetId, 2002L, 3002L, 12L, 2, "0.72", 1, now.minusMinutes(4));
        saveAlert("ALT-OVERVIEW-003", fleetId, 2003L, 3003L, 13L, 1, "0.43", 3, now.minusMinutes(12));

        String token = loginAndGetToken("viewer", "123456");

        mockMvc.perform(get("/api/v1/realtime/overview")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("fleetId", String.valueOf(fleetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fleetId").value(fleetId))
                .andExpect(jsonPath("$.data.alertCountLast5Minutes").value(2))
                .andExpect(jsonPath("$.data.highRiskCountLast5Minutes").value(1))
                .andExpect(jsonPath("$.data.handledCountLast5Minutes").value(1))
                .andExpect(jsonPath("$.data.latestAlerts.length()").value(3))
                .andExpect(jsonPath("$.data.latestAlerts[0].alertNo").value("ALT-OVERVIEW-001"))
                .andExpect(jsonPath("$.data.latestAlerts[1].alertNo").value("ALT-OVERVIEW-002"))
                .andExpect(jsonPath("$.data.riskDistribution.length()").value(3))
                .andExpect(jsonPath("$.data.riskDistribution[0].riskLevel").value(1))
                .andExpect(jsonPath("$.data.riskDistribution[0].count").value(0))
                .andExpect(jsonPath("$.data.riskDistribution[1].riskLevel").value(2))
                .andExpect(jsonPath("$.data.riskDistribution[1].count").value(1))
                .andExpect(jsonPath("$.data.riskDistribution[2].riskLevel").value(3))
                .andExpect(jsonPath("$.data.riskDistribution[2].count").value(1));
    }

    private void saveAlert(String alertNo,
                           Long fleetId,
                           Long vehicleId,
                           Long driverId,
                           Long ruleId,
                           int riskLevel,
                           String riskScore,
                           int status,
                           LocalDateTime triggerTime) {
        AlertEvent alert = new AlertEvent();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        alert.setAlertNo(alertNo);
        alert.setEnterpriseId(enterpriseId);
        alert.setFleetId(fleetId);
        alert.setVehicleId(vehicleId);
        alert.setDriverId(driverId);
        alert.setRuleId(ruleId);
        alert.setRiskLevel((byte) riskLevel);
        alert.setRiskScore(new BigDecimal(riskScore));
        alert.setFatigueScore(new BigDecimal("0.55"));
        alert.setDistractionScore(new BigDecimal("0.44"));
        alert.setTriggerTime(triggerTime);
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

    private Enterprise saveEnterprise(String code, String name, int status) {
        Enterprise enterprise = new Enterprise();
        enterprise.setCode(code);
        enterprise.setName(name);
        enterprise.setStatus((byte) status);
        enterprise.setCreatedAt(LocalDateTime.now());
        enterprise.setUpdatedAt(LocalDateTime.now());
        return enterpriseRepository.save(enterprise);
    }

    private Fleet saveFleet(Long targetEnterpriseId, String name, int status) {
        Fleet fleet = new Fleet();
        fleet.setEnterpriseId(targetEnterpriseId);
        fleet.setName(name);
        fleet.setStatus((byte) status);
        fleet.setCreatedAt(LocalDateTime.now());
        fleet.setUpdatedAt(LocalDateTime.now());
        return fleetRepository.save(fleet);
    }

    private UserAccount saveUser(String username, String password, int status, Long userEnterpriseId) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setSubjectType(SubjectType.USER.name());
        user.setEnterpriseId(userEnterpriseId);
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
