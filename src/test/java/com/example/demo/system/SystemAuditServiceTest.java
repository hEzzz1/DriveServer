package com.example.demo.system;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.system.entity.SystemAuditLog;
import com.example.demo.system.repository.SystemAuditRepository;
import com.example.demo.system.service.SystemAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAuditServiceTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordShouldLeaveOperatorColumnsNullForSystemActions() {
        SystemAuditRepository repository = mock(SystemAuditRepository.class);
        when(repository.save(any(SystemAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SystemAuditService service = new SystemAuditService(repository, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(null, "SESSION", "SIGN_IN_DRIVER", "SESSION", "1", "SUCCESS", "司机签到", List.of("detail"));

        ArgumentCaptor<SystemAuditLog> captor = ArgumentCaptor.forClass(SystemAuditLog.class);
        verify(repository).save(captor.capture());
        SystemAuditLog saved = captor.getValue();
        assertThat(saved.getOperatorId()).isNull();
        assertThat(saved.getActionBy()).isNull();
        assertThat(saved.getOperatorName()).isEqualTo("system");
    }

    @Test
    void recordShouldUseAuthenticatedUserWhenPresent() {
        SystemAuditRepository repository = mock(SystemAuditRepository.class);
        when(repository.save(any(SystemAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SystemAuditService service = new SystemAuditService(repository, new ObjectMapper());

        AuthenticatedUser operator = new AuthenticatedUser(99L, "admin", List.of("SUPER_ADMIN"));
        service.record(operator, "RULE", "CREATE_RULE", "RULE", "42", "SUCCESS", "创建规则", null);

        ArgumentCaptor<SystemAuditLog> captor = ArgumentCaptor.forClass(SystemAuditLog.class);
        verify(repository).save(captor.capture());
        SystemAuditLog saved = captor.getValue();
        assertThat(saved.getOperatorId()).isEqualTo(99L);
        assertThat(saved.getActionBy()).isEqualTo(99L);
        assertThat(saved.getOperatorName()).isEqualTo("admin");
    }
}
