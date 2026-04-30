package com.example.demo.device;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindLog;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindLogRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.model.EnterpriseActivationCodeStatus;
import com.example.demo.enterprise.repository.EnterpriseRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EdgeDeviceClaimIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private EdgeDeviceBindLogRepository edgeDeviceBindLogRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Enterprise enterpriseA;
    private Enterprise enterpriseB;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        edgeDeviceBindLogRepository.deleteAll();
        deviceRepository.deleteAll();
        enterpriseRepository.deleteAll();

        enterpriseA = saveEnterprise("ENT-A", "企业A", "ENT-AAAA-1111");
        enterpriseB = saveEnterprise("ENT-B", "企业B", "ENT-BBBB-2222");

        Role superAdmin = saveRole("SUPER_ADMIN", "超级管理员");
        Role enterpriseAdmin = saveRole("ENTERPRISE_ADMIN", "企业管理员");
        UserAccount superAdminUser = saveUser("super-admin", "123456", null);
        UserAccount enterpriseAdminUser = saveUser("enterprise-admin", "123456", enterpriseA.getId());
        bindUserRole(superAdminUser.getId(), superAdmin.getId());
        bindUserRole(enterpriseAdminUser.getId(), enterpriseAdmin.getId());
    }

    @Test
    void claimShouldBindDeviceAndWriteBindLog() throws Exception {
        mockMvc.perform(post("/api/v1/edge/device/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "EDGE-0001",
                                  "deviceName": "前挡摄像头A",
                                  "enterpriseActivationCode": "ENT-AAAA-1111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device.deviceCode").value("EDGE-0001"))
                .andExpect(jsonPath("$.data.device.deviceToken").isNotEmpty())
                .andExpect(jsonPath("$.data.device.lifecycleStatus").value("BOUND"))
                .andExpect(jsonPath("$.data.enterprise.id").value(enterpriseA.getId()))
                .andExpect(jsonPath("$.data.vehicleBindStatus").value("UNASSIGNED"))
                .andExpect(jsonPath("$.data.effectiveStage").value("WAITING_VEHICLE"));

        Device device = deviceRepository.findByDeviceCode("EDGE-0001").orElseThrow();
        assertThat(device.getEnterpriseId()).isEqualTo(enterpriseA.getId());
        assertThat(device.getStatus()).isEqualTo("BOUND");

        String token = loginAndGetToken("enterprise-admin", "123456");
        mockMvc.perform(get("/api/v1/enterprises/{id}/device-bind-logs", enterpriseA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].deviceCode").value("EDGE-0001"))
                .andExpect(jsonPath("$.data.items[0].action").value("CLAIMED"));
    }

    @Test
    void claimShouldBeIdempotentForSameEnterpriseAndRejectOtherEnterprise() throws Exception {
        mockMvc.perform(post("/api/v1/edge/device/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "EDGE-0002",
                                  "deviceName": "前挡摄像头B",
                                  "enterpriseActivationCode": "ENT-AAAA-1111"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/edge/device/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "EDGE-0002",
                                  "deviceName": "前挡摄像头B",
                                  "enterpriseActivationCode": "ENT-AAAA-1111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterprise.id").value(enterpriseA.getId()));

        mockMvc.perform(post("/api/v1/edge/device/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "EDGE-0002",
                                  "deviceName": "前挡摄像头B",
                                  "enterpriseActivationCode": "ENT-BBBB-2222"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40911));

        assertThat(edgeDeviceBindLogRepository.findAll()).extracting(EdgeDeviceBindLog::getAction)
                .containsExactly("CLAIMED", "AUTO_RECOVERED");
    }

    @Test
    void enterpriseAdminShouldManageOwnActivationCode() throws Exception {
        String token = loginAndGetToken("enterprise-admin", "123456");

        mockMvc.perform(get("/api/v1/enterprises/{id}/activation-code", enterpriseA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activationCode").value("ENT-AAAA-1111"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String rotatedJson = mockMvc.perform(post("/api/v1/enterprises/{id}/activation-code/rotate", enterpriseA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedCode = objectMapper.readTree(rotatedJson).path("data").path("activationCode").asText();
        assertThat(rotatedCode).isNotEqualTo("ENT-AAAA-1111");

        mockMvc.perform(post("/api/v1/enterprises/{id}/activation-code/disable", enterpriseA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    private Enterprise saveEnterprise(String code, String name, String activationCode) {
        Enterprise enterprise = new Enterprise();
        enterprise.setCode(code);
        enterprise.setName(name);
        enterprise.setStatus((byte) 1);
        enterprise.setActivationCode(activationCode);
        enterprise.setActivationCodeStatus(EnterpriseActivationCodeStatus.ACTIVE.name());
        enterprise.setActivationCodeRotatedAt(LocalDateTime.now());
        enterprise.setCreatedAt(LocalDateTime.now());
        enterprise.setUpdatedAt(LocalDateTime.now());
        return enterpriseRepository.save(enterprise);
    }

    private Role saveRole(String roleCode, String roleName) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    private UserAccount saveUser(String username, String password, Long enterpriseId) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setSubjectType(SubjectType.USER.name());
        user.setEnterpriseId(enterpriseId);
        user.setStatus((byte) 1);
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
