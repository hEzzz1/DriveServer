package com.example.demo.rule;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.entity.UserScopeRole;
import com.example.demo.auth.model.RoleTemplateCode;
import com.example.demo.auth.model.ScopeType;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.auth.repository.UserScopeRoleRepository;
import com.example.demo.rule.repository.RuleConfigRepository;
import com.example.demo.rule.repository.RuleConfigVersionRepository;
import com.example.demo.system.repository.SystemAuditRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RuleManagementModuleIntegrationTest {

    private static final long ENTERPRISE_ID = 1001L;

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
    private UserScopeRoleRepository userScopeRoleRepository;

    @Autowired
    private RuleConfigRepository ruleConfigRepository;

    @Autowired
    private RuleConfigVersionRepository ruleConfigVersionRepository;

    @Autowired
    private SystemAuditRepository systemAuditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        systemAuditRepository.deleteAll();
        ruleConfigVersionRepository.deleteAll();
        ruleConfigRepository.deleteAll();
        userScopeRoleRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role admin = saveRole("SUPER_ADMIN", "超级管理员");
        Role viewer = saveRole("VIEWER", "只读观察员");

        UserAccount adminUser = saveUser("admin", "123456", 1, null);
        UserAccount viewerUser = saveUser("viewer", "123456", 1, ENTERPRISE_ID);
        bindUserRole(adminUser.getId(), admin.getId());
        bindUserRole(viewerUser.getId(), viewer.getId());
        bindScopeRole(adminUser.getId(), RoleTemplateCode.PLATFORM_SUPER_ADMIN.name(), null);
        bindScopeRole(viewerUser.getId(), RoleTemplateCode.ORG_VIEWER.name(), ENTERPRISE_ID);
    }

    @Test
    void adminShouldManageRuleLifecycleAndWriteAuditLogs() throws Exception {
        String adminToken = loginAndGetToken("admin", "123456");
        String viewerToken = loginAndGetToken("viewer", "123456");

        mockMvc.perform(post("/api/v1/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_CUSTOM_ALPHA",
                                  "ruleName": "自定义规则A",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.70,
                                  "durationSeconds": 3,
                                  "cooldownSeconds": 60,
                                  "enabled": false,
                                  "status": "DRAFT",
                                  "changeRemark": "创建草稿"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        Long ruleId = ruleConfigRepository.findByRuleCode("RULE_CUSTOM_ALPHA")
                .orElseThrow()
                .getId();

                mockMvc.perform(get("/api/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleCode").value("RULE_CUSTOM_ALPHA"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.versions.length()").value(0));

        mockMvc.perform(post("/api/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeRemark": "首次发布"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(put("/api/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_CUSTOM_ALPHA",
                                  "ruleName": "自定义规则A-V2",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.74,
                                  "durationSeconds": 4,
                                  "cooldownSeconds": 90,
                                  "enabled": true,
                                  "status": "ENABLED",
                                  "changeRemark": "草稿更新"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(post("/api/v1/rules/{id}/toggle", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_CUSTOM_ALPHA",
                                  "ruleName": "自定义规则A-V2",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.74,
                                  "durationSeconds": 4,
                                  "cooldownSeconds": 90,
                                  "enabled": false,
                                  "status": "DRAFT",
                                  "changeRemark": "草稿更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeRemark": "二次发布"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(3));

        mockMvc.perform(post("/api/v1/rules/{id}/rollback", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNo": 1,
                                  "changeRemark": "回滚到首版"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(4));

        mockMvc.perform(get("/api/v1/rules/{id}/versions", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].versionNo").value(4))
                .andExpect(jsonPath("$.data[3].versionNo").value(1));

        mockMvc.perform(get("/api/v1/audits")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("module", "RULE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items.length()").value(6));

        mockMvc.perform(get("/api/v1/rules")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/system/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.health.status").isNotEmpty())
                .andExpect(jsonPath("$.data.services.items.length()").value(5))
                .andExpect(jsonPath("$.data.monitoring.enabledRuleCount").value(1));

        String exportJson = mockMvc.perform(get("/api/v1/audits/export")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("module", "RULE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode exportRoot = objectMapper.readTree(exportJson);
        assertThat(exportRoot.path("data").path("items")).isNotNull();
    }

    @Test
    void createShouldRejectDirectEnableAndPublishShouldRejectDuplicateRiskLevel() throws Exception {
        String adminToken = loginAndGetToken("admin", "123456");

        mockMvc.perform(post("/api/v1/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_DIRECT_ENABLE",
                                  "ruleName": "非法直开规则",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.70,
                                  "durationSeconds": 3,
                                  "cooldownSeconds": 60,
                                  "enabled": true,
                                  "status": "ENABLED",
                                  "changeRemark": "非法创建"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(post("/api/v1/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_HIGH_A",
                                  "ruleName": "高风险规则A",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.70,
                                  "durationSeconds": 3,
                                  "cooldownSeconds": 60,
                                  "enabled": false,
                                  "status": "DRAFT",
                                  "changeRemark": "创建A"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleCode": "RULE_HIGH_B",
                                  "ruleName": "高风险规则B",
                                  "riskLevel": 3,
                                  "riskThreshold": 0.75,
                                  "durationSeconds": 4,
                                  "cooldownSeconds": 60,
                                  "enabled": false,
                                  "status": "DRAFT",
                                  "changeRemark": "创建B"
                                }
                                """))
                .andExpect(status().isOk());

        Long ruleAId = ruleConfigRepository.findByRuleCode("RULE_HIGH_A").orElseThrow().getId();
        Long ruleBId = ruleConfigRepository.findByRuleCode("RULE_HIGH_B").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/rules/{id}/publish", ruleAId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeRemark": "发布A"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(post("/api/v1/rules/{id}/publish", ruleBId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeRemark": "发布B"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private Role saveRole(String roleCode, String roleName) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    private UserAccount saveUser(String username, String password, int status, Long enterpriseId) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setSubjectType(SubjectType.USER.name());
        user.setEnterpriseId(enterpriseId);
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

    private void bindScopeRole(Long userId, String roleCode, Long enterpriseId) {
        UserScopeRole role = new UserScopeRole();
        role.setUserId(userId);
        role.setRoleCode(roleCode);
        role.setScopeType(RoleTemplateCode.from(roleCode).orElseThrow().isPlatformRole()
                ? ScopeType.PLATFORM.name()
                : ScopeType.ENTERPRISE.name());
        role.setEnterpriseId(enterpriseId);
        role.setStatus((byte) 1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userScopeRoleRepository.save(role);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
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
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }
}
