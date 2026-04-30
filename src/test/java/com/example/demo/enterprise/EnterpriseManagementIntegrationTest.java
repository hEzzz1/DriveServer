package com.example.demo.enterprise;

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
import com.example.demo.enterprise.entity.Enterprise;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnterpriseManagementIntegrationTest {

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
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Enterprise enterpriseA;
    private Enterprise enterpriseB;
    private UserAccount superAdminUser;
    private UserAccount sysAdminUser;
    private UserAccount enterpriseAdminUser;

    @BeforeEach
    void setUp() {
        userScopeRoleRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        enterpriseRepository.deleteAll();

        enterpriseA = saveEnterprise("ENT-A", "企业A", 1);
        enterpriseB = saveEnterprise("ENT-B", "企业B", 1);

        Role superAdmin = saveRole("SUPER_ADMIN", "超级管理员");
        Role sysAdmin = saveRole("SYS_ADMIN", "系统管理员");
        Role enterpriseAdmin = saveRole("ENTERPRISE_ADMIN", "企业管理员");

        superAdminUser = saveUser("super-admin", "123456", 1, null);
        sysAdminUser = saveUser("sys-admin", "123456", 1, null);
        enterpriseAdminUser = saveUser("enterprise-admin", "123456", 1, enterpriseA.getId());

        bindUserRole(superAdminUser.getId(), superAdmin.getId());
        bindUserRole(sysAdminUser.getId(), sysAdmin.getId());
        bindUserRole(enterpriseAdminUser.getId(), enterpriseAdmin.getId());
        bindScopeRole(superAdminUser.getId(), RoleTemplateCode.PLATFORM_SUPER_ADMIN.name(), null);
        bindScopeRole(sysAdminUser.getId(), RoleTemplateCode.PLATFORM_SYS_ADMIN.name(), null);
        bindScopeRole(enterpriseAdminUser.getId(), RoleTemplateCode.ORG_ADMIN.name(), enterpriseA.getId());
    }

    @Test
    void superAdminShouldManageEnterprises() throws Exception {
        String token = loginAndGetToken("super-admin", "123456");

        mockMvc.perform(post("/api/v1/platform/enterprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ENT-C",
                                  "name": "企业C",
                                  "remark": "测试企业"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ENT-C"))
                .andExpect(jsonPath("$.data.name").value("企业C"));

        mockMvc.perform(put("/api/v1/platform/enterprises/{id}/status", enterpriseB.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void sysAdminShouldNotParticipateEnterpriseMasterData() throws Exception {
        String token = loginAndGetToken("sys-admin", "123456");

        mockMvc.perform(get("/api/v1/platform/enterprises")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(post("/api/v1/platform/enterprises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ENT-X",
                                  "name": "企业X"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void enterpriseAdminShouldReadOwnEnterpriseProfileAndBeBlockedFromPlatformEnterpriseApis() throws Exception {
        String token = loginAndGetToken("enterprise-admin", "123456");

        mockMvc.perform(get("/api/v1/org/enterprise-profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(enterpriseA.getId()))
                .andExpect(jsonPath("$.data.code").value("ENT-A"));

        mockMvc.perform(get("/api/v1/platform/enterprises/{id}", enterpriseB.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
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
