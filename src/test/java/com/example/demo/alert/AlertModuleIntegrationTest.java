package com.example.demo.alert;

import com.example.demo.alert.repository.AlertActionLogRepository;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.SubjectType;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private AlertActionLogRepository alertActionLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        alertActionLogRepository.deleteAll();
        alertEventRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role admin = saveRole("SUPER_ADMIN", "超级管理员");
        Role operator = saveRole("OPERATOR", "运维操作员");
        Role viewer = saveRole("VIEWER", "观察员");

        UserAccount adminUser = saveUser("admin", "123456", 1);
        UserAccount operatorUser = saveUser("operator", "123456", 1);
        UserAccount viewerUser = saveUser("viewer", "123456", 1);

        bindUserRole(adminUser.getId(), admin.getId());
        bindUserRole(operatorUser.getId(), operator.getId());
        bindUserRole(viewerUser.getId(), viewer.getId());
    }

    @Test
    void createConfirmCloseShouldWriteOperationLogs() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");
        String viewerToken = loginAndGetToken("viewer", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(post("/api/v1/alerts/{id}/confirm", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "已联系司机"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.actionType").value("CONFIRM"));

        mockMvc.perform(post("/api/v1/alerts/{id}/close", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "风险已解除"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(3))
                .andExpect(jsonPath("$.data.actionType").value("CLOSE"));

        mockMvc.perform(get("/api/v1/alerts/{id}/action-logs", alertId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").isNumber())
                .andExpect(jsonPath("$.data.items[0].actionType").value("CREATE"))
                .andExpect(jsonPath("$.data.items[0].actionBy").isNumber())
                .andExpect(jsonPath("$.data.items[0].actionTime").isNotEmpty())
                .andExpect(jsonPath("$.data.items[1].actionType").value("CONFIRM"))
                .andExpect(jsonPath("$.data.items[2].actionType").value("CLOSE"));
    }

    @Test
    void falsePositiveShouldWriteOperationLog() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(post("/api/v1/alerts/{id}/false-positive", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "遮挡导致误报"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.actionType").value("FALSE_POSITIVE"));

        mockMvc.perform(get("/api/v1/alerts/{id}/action-logs", alertId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].actionType").value("CREATE"))
                .andExpect(jsonPath("$.data.items[1].actionType").value("FALSE_POSITIVE"));
    }

    @Test
    void viewerShouldNotOperateAlert() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");
        String viewerToken = loginAndGetToken("viewer", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(post("/api/v1/alerts/{id}/confirm", alertId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "viewer操作"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void closedAlertShouldRejectFurtherTransition() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(post("/api/v1/alerts/{id}/close", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "先关闭"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/alerts/{id}/confirm", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "关闭后确认"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("当前状态不允许该操作"));
    }

    @Test
    void falsePositiveAlertShouldRejectFurtherTransitionWithStableBusinessError() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(post("/api/v1/alerts/{id}/false-positive", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "判定误报"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/alerts/{id}/close", alertId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "误报后尝试关闭"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("当前状态不允许该操作"));
    }

    @Test
    void listAlertsShouldReturnPagedData() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");
        String viewerToken = loginAndGetToken("viewer", "123456");

        createAlert(operatorToken);

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + viewerToken)
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value(0));
    }

    @Test
    void alertDetailShouldBeAccessibleByViewer() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");
        String viewerToken = loginAndGetToken("viewer", "123456");

        Long alertId = createAlert(operatorToken);

        mockMvc.perform(get("/api/v1/alerts/{id}", alertId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(alertId))
                .andExpect(jsonPath("$.data.alertNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void methodNotAllowedShouldReturn405InsteadOf500() throws Exception {
        String operatorToken = loginAndGetToken("operator", "123456");
        Long alertId = createAlert(operatorToken);

        mockMvc.perform(put("/api/v1/alerts/{id}/close", alertId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40501));
    }

    private Long createAlert(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAlertPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.actionType").value("CREATE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("id").asLong();
    }

    private String createAlertPayload() {
        return """
                {
                  "fleetId": 1001,
                  "vehicleId": 2001,
                  "driverId": 3001,
                  "ruleId": 1,
                  "riskLevel": 3,
                  "riskScore": 0.89,
                  "fatigueScore": 0.91,
                  "distractionScore": 0.86,
                  "triggerTime": "2026-04-07T10:01:16Z",
                  "remark": "系统自动创建"
                }
                """;
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
        user.setSubjectType(SubjectType.USER.name());
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
