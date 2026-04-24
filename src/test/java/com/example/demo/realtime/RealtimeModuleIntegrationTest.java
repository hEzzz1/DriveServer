package com.example.demo.realtime;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeModuleIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private Long operatorUserId;

    @BeforeEach
    void setUp() {
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userAccountRepository.deleteAll();

        Role operator = saveRole("OPERATOR", "运维操作员");
        UserAccount operatorUser = saveUser("operator", "123456", 1);
        bindUserRole(operatorUser.getId(), operator.getId());
        operatorUserId = operatorUser.getId();
    }

    @Test
    void shouldPushStandardRealtimeMessagesForCreateAndStateChanges() throws Exception {
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

        Long confirmedAlertId = createAlert(token, "系统自动创建");
        assertCreatedMessage(nextMessage(queue), confirmedAlertId, "系统自动创建");
        confirmAlert(confirmedAlertId, token, "已联系司机");
        assertUpdatedMessage(nextMessage(queue), confirmedAlertId, 1, "已联系司机");

        Long falsePositiveAlertId = createAlert(token, "待人工核验");
        assertCreatedMessage(nextMessage(queue), falsePositiveAlertId, "待人工核验");
        markFalsePositive(falsePositiveAlertId, token, "已判定误报");
        assertUpdatedMessage(nextMessage(queue), falsePositiveAlertId, 2, "已判定误报");

        Long closedAlertId = createAlert(token, "边缘端重复提醒");
        assertCreatedMessage(nextMessage(queue), closedAlertId, "边缘端重复提醒");
        closeAlert(closedAlertId, token, "风险解除，关闭告警");
        assertUpdatedMessage(nextMessage(queue), closedAlertId, 3, "风险解除，关闭告警");

        session.disconnect();
        stompClient.stop();
    }

    private AlertRealtimeMessage nextMessage(LinkedBlockingQueue<AlertRealtimeMessage> queue) throws InterruptedException {
        AlertRealtimeMessage message = queue.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.getTraceId()).isNotBlank();
        assertThat(message.getData()).isNotNull();
        return message;
    }

    private void assertCreatedMessage(AlertRealtimeMessage message, Long alertId, String remark) {
        assertThat(message.getEventType()).isEqualTo("ALERT_CREATED");
        assertRealtimePayload(message, alertId, 0, remark);
    }

    private void assertUpdatedMessage(AlertRealtimeMessage message, Long alertId, int expectedStatus, String remark) {
        assertThat(message.getEventType()).isEqualTo("ALERT_UPDATED");
        assertRealtimePayload(message, alertId, expectedStatus, remark);
    }

    private void assertRealtimePayload(AlertRealtimeMessage message, Long alertId, int expectedStatus, String expectedRemark) {
        assertThat(message.getData().getAlertId()).isEqualTo(alertId);
        assertThat(message.getData().getAlertNo()).startsWith("ALT");
        assertThat(message.getData().getStatus()).isEqualTo(expectedStatus);
        assertThat(message.getData().getRiskLevel()).isEqualTo(3);
        assertThat(message.getData().getRiskScore()).isEqualByComparingTo(new BigDecimal("0.89"));
        assertThat(message.getData().getFatigueScore()).isEqualByComparingTo(new BigDecimal("0.91"));
        assertThat(message.getData().getDistractionScore()).isEqualByComparingTo(new BigDecimal("0.86"));
        assertThat(message.getData().getTriggerTime()).isEqualTo(OffsetDateTime.parse("2026-04-07T10:01:16Z"));
        assertThat(message.getData().getFleetId()).isEqualTo(1001L);
        assertThat(message.getData().getVehicleId()).isEqualTo(2001L);
        assertThat(message.getData().getDriverId()).isEqualTo(3001L);
        assertThat(message.getData().getLatestActionBy()).isEqualTo(operatorUserId);
        assertThat(message.getData().getLatestActionTime()).isNotNull();
        assertThat(message.getData().getRemark()).isEqualTo(expectedRemark);
    }

    private Long createAlert(String token, String remark) throws Exception {
        HttpHeaders headers = jsonHeaders(token);
        HttpEntity<String> request = new HttpEntity<>(createAlertPayload(remark), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/alerts"),
                request,
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(0);
        return root.path("data").path("id").asLong();
    }

    private void confirmAlert(Long alertId, String token, String remark) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/alerts/" + alertId + "/confirm"),
                actionRequest(token, remark),
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void markFalsePositive(Long alertId, String token, String remark) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/alerts/" + alertId + "/false-positive"),
                actionRequest(token, remark),
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void closeAlert(Long alertId, String token, String remark) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/alerts/" + alertId + "/close"),
                actionRequest(token, remark),
                String.class);
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

    private HttpEntity<String> actionRequest(String token, String remark) {
        return new HttpEntity<>("""
                {
                  "remark": "%s"
                }
                """.formatted(remark), jsonHeaders(token));
    }

    private String createAlertPayload(String remark) {
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
                  "remark": "%s"
                }
                """.formatted(remark);
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
}
