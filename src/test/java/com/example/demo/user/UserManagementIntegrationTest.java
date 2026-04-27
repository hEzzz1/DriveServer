package com.example.demo.user;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
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
class UserManagementIntegrationTest {

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
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount superAdminUser;
    private UserAccount enterpriseAdminAUser;
    private UserAccount enterpriseViewerAUser;
    private UserAccount enterpriseViewerBUser;
    private Enterprise enterpriseA;
    private Enterprise enterpriseB;
    private Enterprise platformEnterprise;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        enterpriseRepository.deleteAll();

        enterpriseA = saveEnterprise("ENT-100", "企业A", 1);
        enterpriseB = saveEnterprise("ENT-200", "企业B", 1);
        platformEnterprise = saveEnterprise("ENT-999", "平台企业", 1);

        Role superAdmin = saveRole("SUPER_ADMIN", "超级管理员");
        Role enterpriseAdmin = saveRole("ENTERPRISE_ADMIN", "企业管理员");
        Role riskAdmin = saveRole("RISK_ADMIN", "风控管理员");
        Role operator = saveRole("OPERATOR", "操作员");
        Role viewer = saveRole("VIEWER", "观察员");
        saveRole("ANALYST", "分析员");
        saveRole("SYS_ADMIN", "系统管理员");

        superAdminUser = saveUser("super-admin", "123456", 1, platformEnterprise.getId());
        enterpriseAdminAUser = saveUser("enterprise-admin-a", "123456", 1, enterpriseA.getId());
        enterpriseViewerAUser = saveUser("viewer-a", "123456", 1, enterpriseA.getId());
        enterpriseViewerBUser = saveUser("viewer-b", "123456", 1, enterpriseB.getId());

        bindUserRole(superAdminUser.getId(), superAdmin.getId());
        bindUserRole(enterpriseAdminAUser.getId(), enterpriseAdmin.getId());
        bindUserRole(enterpriseViewerAUser.getId(), viewer.getId());
        bindUserRole(enterpriseViewerBUser.getId(), viewer.getId());
        bindUserRole(enterpriseViewerAUser.getId(), operator.getId());
        bindUserRole(enterpriseViewerBUser.getId(), riskAdmin.getId());
    }

    @Test
    void superAdminCanListAllUsersAndFilterByEnterprise() throws Exception {
        String token = loginAndGetToken("super-admin", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("enterpriseId", String.valueOf(enterpriseA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].enterpriseId").value(enterpriseA.getId()))
                .andExpect(jsonPath("$.data.items[1].enterpriseId").value(enterpriseA.getId()));
    }

    @Test
    void enterpriseAdminShouldOnlySeeOwnEnterpriseAndIgnoreExternalFilter() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("enterpriseId", String.valueOf(enterpriseB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].enterpriseId").value(enterpriseA.getId()))
                .andExpect(jsonPath("$.data.items[1].enterpriseId").value(enterpriseA.getId()));
    }

    @Test
    void enterpriseAdminShouldBeForbiddenToReadOtherEnterpriseUser() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(get("/api/v1/users/{id}", enterpriseViewerBUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void enterpriseAdminShouldCreateUserInOwnEnterprise() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user-a",
                                  "password": "123456",
                                  "nickname": "new-user-a",
                                  "enterpriseId": %d,
                                  "enabled": true,
                                  "roles": ["VIEWER"]
                                }
                                """.formatted(enterpriseA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new-user-a"))
                .andExpect(jsonPath("$.data.enterpriseId").value(enterpriseA.getId()))
                .andExpect(jsonPath("$.data.roles[0]").value("VIEWER"));
    }

    @Test
    void enterpriseAdminShouldRejectCreatingUserForAnotherEnterprise() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user-bad",
                                  "password": "123456",
                                  "nickname": "new-user-bad",
                                  "enterpriseId": %d,
                                  "enabled": true,
                                  "roles": ["VIEWER"]
                                }
                                """.formatted(enterpriseB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void enterpriseAdminShouldOnlyAssignWhitelistedRoles() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(post("/api/v1/users/{id}/roles", enterpriseViewerAUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["OPERATOR", "VIEWER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.data.roles[1]").value("VIEWER"));

        mockMvc.perform(put("/api/v1/users/{id}/roles", enterpriseViewerAUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["SYS_ADMIN"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void enterpriseAdminShouldSeeFilteredRoleList() throws Exception {
        String token = loginAndGetToken("enterprise-admin-a", "123456");

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].roleCode").value("ANALYST"))
                .andExpect(jsonPath("$.data[1].roleCode").value("OPERATOR"))
                .andExpect(jsonPath("$.data[2].roleCode").value("RISK_ADMIN"))
                .andExpect(jsonPath("$.data[3].roleCode").value("VIEWER"));
    }

    @Test
    void disabledUserTokenShouldBecomeInvalidOnNextRequest() throws Exception {
        String viewerToken = loginAndGetToken("viewer-a", "123456");
        String superAdminToken = loginAndGetToken("super-admin", "123456");

        mockMvc.perform(put("/api/v1/users/{id}/status", enterpriseViewerAUser.getId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
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
