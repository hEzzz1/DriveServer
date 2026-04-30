package com.example.demo.driver;

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
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DriverManagementIntegrationTest {

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
    private FleetRepository fleetRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Enterprise enterpriseA;
    private Enterprise enterpriseB;
    private Fleet fleetA1;
    private Fleet fleetA2;
    private Fleet fleetB1;
    private Driver driverA;

    @BeforeEach
    void setUp() {
        userScopeRoleRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();
        driverRepository.deleteAll();
        fleetRepository.deleteAll();
        enterpriseRepository.deleteAll();

        enterpriseA = saveEnterprise("ENT-A", "企业A", 1);
        enterpriseB = saveEnterprise("ENT-B", "企业B", 1);
        fleetA1 = saveFleet(enterpriseA.getId(), "A车队1", 1);
        fleetA2 = saveFleet(enterpriseA.getId(), "A车队2", 1);
        fleetB1 = saveFleet(enterpriseB.getId(), "B车队1", 1);
        driverA = saveDriver(enterpriseA.getId(), fleetA1.getId(), "张三", "13800000000", "LIC-A", 1);

        Role superAdmin = saveRole("SUPER_ADMIN", "超级管理员");
        Role enterpriseAdmin = saveRole("ENTERPRISE_ADMIN", "企业管理员");
        Role analyst = saveRole("ANALYST", "分析员");

        UserAccount superAdminUser = saveUser("super-admin", "123456", 1, null);
        UserAccount enterpriseAdminUser = saveUser("enterprise-admin", "123456", 1, enterpriseA.getId());
        UserAccount analystUser = saveUser("analyst-user", "123456", 1, enterpriseA.getId());

        bindUserRole(superAdminUser.getId(), superAdmin.getId());
        bindUserRole(enterpriseAdminUser.getId(), enterpriseAdmin.getId());
        bindUserRole(analystUser.getId(), analyst.getId());
        bindScopeRole(superAdminUser.getId(), RoleTemplateCode.PLATFORM_SUPER_ADMIN.name(), null);
        bindScopeRole(enterpriseAdminUser.getId(), RoleTemplateCode.ORG_ADMIN.name(), enterpriseA.getId());
        bindScopeRole(analystUser.getId(), RoleTemplateCode.ORG_ANALYST.name(), enterpriseA.getId());
    }

    @Test
    void createDriverShouldRejectCrossEnterpriseFleetBinding() throws Exception {
        String token = loginAndGetToken("enterprise-admin", "123456");

        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterpriseId": %d,
                                  "fleetId": %d,
                                  "name": "李四",
                                  "phone": "13900000000",
                                  "licenseNo": "LIC-B"
                                }
                                """.formatted(enterpriseA.getId(), fleetB1.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void reassignDriverShouldStayWithinSameEnterprise() throws Exception {
        String token = loginAndGetToken("enterprise-admin", "123456");

        mockMvc.perform(put("/api/v1/drivers/{id}/fleet", driverA.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fleetId": %d
                                }
                                """.formatted(fleetA2.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fleetId").value(fleetA2.getId()));

        mockMvc.perform(put("/api/v1/drivers/{id}/fleet", driverA.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fleetId": %d
                                }
                                """.formatted(fleetB1.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void analystShouldReadOwnEnterpriseDriversOnly() throws Exception {
        String token = loginAndGetToken("analyst-user", "123456");

        mockMvc.perform(get("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(driverA.getId()));

        mockMvc.perform(get("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .param("enterpriseId", String.valueOf(enterpriseB.getId())))
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

    private Fleet saveFleet(Long enterpriseId, String name, int status) {
        Fleet fleet = new Fleet();
        fleet.setEnterpriseId(enterpriseId);
        fleet.setName(name);
        fleet.setStatus((byte) status);
        fleet.setCreatedAt(LocalDateTime.now());
        fleet.setUpdatedAt(LocalDateTime.now());
        return fleetRepository.save(fleet);
    }

    private Driver saveDriver(Long enterpriseId, Long fleetId, String name, String phone, String licenseNo, int status) {
        Driver driver = new Driver();
        driver.setEnterpriseId(enterpriseId);
        driver.setFleetId(fleetId);
        driver.setName(name);
        driver.setPhone(phone);
        driver.setLicenseNo(licenseNo);
        driver.setStatus((byte) status);
        driver.setCreatedAt(LocalDateTime.now());
        driver.setUpdatedAt(LocalDateTime.now());
        return driverRepository.save(driver);
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
