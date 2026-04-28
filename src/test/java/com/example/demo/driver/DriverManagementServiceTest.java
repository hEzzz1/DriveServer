package com.example.demo.driver;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.driver.dto.DriverPageResponseData;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.driver.service.DriverManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverManagementServiceTest {

    @Test
    void listDriversShouldIncludeActiveSession() {
        DriverRepository driverRepository = mock(DriverRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        DriverManagementService service = new DriverManagementService(
                driverRepository,
                enterpriseRepository,
                fleetRepository,
                sessionRepository,
                businessAccessService,
                systemAuditService,
                passwordEncoder);

        Driver driver = new Driver();
        driver.setId(5L);
        driver.setEnterpriseId(10L);
        driver.setFleetId(20L);
        driver.setDriverCode("DRV001");
        driver.setName("张三");
        driver.setStatus((byte) 1);

        DrivingSession activeSession = new DrivingSession();
        activeSession.setId(88L);
        activeSession.setDriverId(5L);
        activeSession.setStatus(SessionStatus.ACTIVE.getCode());

        when(businessAccessService.resolveReadableEnterpriseId(any(), eq(null))).thenReturn(null);
        when(driverRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(driver)));
        when(sessionRepository.findByStatusAndDriverIdInOrderBySignInTimeDesc(SessionStatus.ACTIVE.getCode(), List.of(5L)))
                .thenReturn(List.of(activeSession));

        DriverPageResponseData result = service.listDrivers(operator(), 1, 20, null, null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).hasActiveSession()).isTrue();
        assertThat(result.items().get(0).activeSessionId()).isEqualTo(88L);
    }

    private AuthenticatedUser operator() {
        return new AuthenticatedUser(1L, "admin", List.of("SUPER_ADMIN"));
    }
}
