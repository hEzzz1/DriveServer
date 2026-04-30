package com.example.demo.realtime;

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
import com.example.demo.realtime.dto.AlertRealtimeMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeModuleIntegrationTest {

    private static final long ENTERPRISE_ID = 1001L;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userScopeRoleRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role operator = saveRole("OPERATOR", "运维操作员");
        UserAccount operatorUser = saveUser("operator", "123456", 1, ENTERPRISE_ID);
        bindUserRole(operatorUser.getId(), operator.getId());
        bindScopeRole(operatorUser.getId(), RoleTemplateCode.ORG_OPERATOR.name(), ENTERPRISE_ID);
    }

    @Test
    void shouldPushAlertCreatedAndUpdatedMessages() throws Exception {
        String token = loginAndGetToken("operator", "123456");

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
        stompClient.setMessageConverter(messageConverter);

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        String wsUrl = "ws://localhost:" + port + "/ws/alerts";
        StompSession session = stompClient.connectAsync(
                        wsUrl,
                        handshakeHeaders,
                        new StompHeaders(),
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<AlertRealtimeMessage> queue = new LinkedBlockingQueue<>();
        session.subscribe("/topic/alerts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AlertRealtimeMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((AlertRealtimeMessage) payload);
            }
        });
        Thread.sleep(300L);

        Long alertId = createAlert(token);
        AlertRealtimeMessage created = queue.poll(5, TimeUnit.SECONDS);
        assertThat(created).isNotNull();
        assertThat(created.getType()).isEqualTo("ALERT_CREATED");
        assertThat(created.getPayload().getAlertId()).isEqualTo(alertId);
        assertThat(created.getPayload().getActionType()).isEqualTo("CREATE");
        assertThat(created.getPayload().getStatus()).isEqualTo(0);

        confirmAlert(alertId, token);
        AlertRealtimeMessage updated = queue.poll(5, TimeUnit.SECONDS);
        assertThat(updated).isNotNull();
        assertThat(updated.getType()).isEqualTo("ALERT_UPDATED");
        assertThat(updated.getPayload().getAlertId()).isEqualTo(alertId);
        assertThat(updated.getPayload().getActionType()).isEqualTo("CONFIRM");
        assertThat(updated.getPayload().getStatus()).isEqualTo(1);

        session.disconnect();
        stompClient.stop();
    }

    private Long createAlert(String token) throws Exception {
        HttpHeaders headers = jsonHeaders(token);
        HttpEntity<String> request = new HttpEntity<>(createAlertPayload(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/alerts"),
                request,
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(0);
        return root.path("data").path("id").asLong();
    }

    private void confirmAlert(Long alertId, String token) {
        HttpHeaders headers = jsonHeaders(token);
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "remark": "已联系司机"
                }
                """, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/v1/alerts/" + alertId + "/confirm"), request, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/v1/auth/login"), request, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(0);
        return root.path("data").path("token").asText();
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String createAlertPayload() {
        return """
                {
                  "enterpriseId": %d,
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
                """.formatted(ENTERPRISE_ID);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
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
}
