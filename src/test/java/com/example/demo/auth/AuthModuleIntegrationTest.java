package com.example.demo.auth;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthModuleIntegrationTest {

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

    @BeforeEach
    void setUp() {
        userScopeRoleRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        enterpriseRepository.deleteAll();

        Enterprise enterprise = new Enterprise();
        enterprise.setCode("ENT-A");
        enterprise.setName("企业A");
        enterprise.setStatus((byte) 1);
        enterprise.setCreatedAt(LocalDateTime.now());
        enterprise.setUpdatedAt(LocalDateTime.now());
        Enterprise savedEnterprise = enterpriseRepository.save(enterprise);

        Role admin = saveRole("SUPER_ADMIN", "超级管理员");
        Role viewer = saveRole("VIEWER", "观察员");
        Role enterpriseAdmin = saveRole("ENTERPRISE_ADMIN", "企业管理员");

        UserAccount adminUser = saveUser("admin", "123456", 1, null);
        UserAccount viewerUser = saveUser("viewer", "123456", 1, savedEnterprise.getId());
        UserAccount enterpriseAdminUser = saveUser("enterprise-admin", "123456", 1, savedEnterprise.getId());

        bindUserRole(adminUser.getId(), admin.getId());
        bindUserRole(viewerUser.getId(), viewer.getId());
        bindUserRole(enterpriseAdminUser.getId(), enterpriseAdmin.getId());
        bindScopeRole(adminUser.getId(), RoleTemplateCode.PLATFORM_SUPER_ADMIN.name(), null);
        bindScopeRole(viewerUser.getId(), RoleTemplateCode.ORG_VIEWER.name(), savedEnterprise.getId());
        bindScopeRole(enterpriseAdminUser.getId(), RoleTemplateCode.ORG_ADMIN.name(), savedEnterprise.getId());
    }

    @Test
    void loginShouldReturnTokenAndRoles() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.roles[0]").value("PLATFORM_SUPER_ADMIN"));
    }

    @Test
    void loginShouldFailWhenPasswordInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "bad-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void meShouldRequireValidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        String token = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.subjectType").value("USER"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void meShouldAllowEnterpriseAdmin() throws Exception {
        String token = loginAndGetToken("enterprise-admin", "123456");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("enterprise-admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("ORG_ADMIN"));
    }

    @Test
    void adminEndpointShouldRejectViewer() throws Exception {
        String viewerToken = loginAndGetToken("viewer", "123456");
        mockMvc.perform(get("/api/v1/auth/admin/ping")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        String adminToken = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/v1/auth/admin/ping")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void corsPreflightShouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
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
