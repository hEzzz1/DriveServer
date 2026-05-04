package com.example.demo.session;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.service.DeviceAuthContext;
import com.example.demo.device.service.DeviceService;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import com.example.demo.session.dto.SessionAdminPageResponseData;
import com.example.demo.session.dto.SignInSessionRequest;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.session.service.DrivingSessionService;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DrivingSessionServiceTest {

    @Test
    void listSessionsShouldNotPretendUpdatedAtIsHeartbeat() {
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DeviceService deviceService = mock(DeviceService.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DrivingSessionService service = new DrivingSessionService(
                sessionRepository,
                driverRepository,
                deviceRepository,
                enterpriseRepository,
                fleetRepository,
                vehicleRepository,
                deviceService,
                businessAccessService,
                passwordEncoder,
                systemAuditService,
                edgeConfigVersionResolver);

        DrivingSession session = buildSession();
        when(businessAccessService.resolveReadableEnterpriseId(any(), eq(null))).thenReturn(null);
        when(sessionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(enterpriseRepository.findAllById(List.of(10L))).thenReturn(List.of(enterprise(10L, "ent")));
        when(fleetRepository.findAllById(List.of(20L))).thenReturn(List.of(fleet(20L, "fleet")));
        when(vehicleRepository.findAllById(List.of(30L))).thenReturn(List.of(vehicle(30L, "沪A12345")));
        when(driverRepository.findAllById(List.of(40L))).thenReturn(List.of(driver(40L, "D001", "driver")));
        when(deviceRepository.findAllById(List.of(50L))).thenReturn(List.of(device(50L, "DEV001")));

        SessionAdminPageResponseData result = service.listSessions(operator(), 1, 20, null, null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).lastHeartbeatAt()).isNull();
    }

    @Test
    void forceSignOutShouldRejectRemarkLongerThanColumn() {
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DeviceService deviceService = mock(DeviceService.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DrivingSessionService service = new DrivingSessionService(
                sessionRepository,
                driverRepository,
                deviceRepository,
                enterpriseRepository,
                fleetRepository,
                vehicleRepository,
                deviceService,
                businessAccessService,
                passwordEncoder,
                systemAuditService,
                edgeConfigVersionResolver);

        DrivingSession session = buildSession();
        when(sessionRepository.findById(1L)).thenReturn(java.util.Optional.of(session));

        assertThatThrownBy(() -> service.forceSignOut(operator(), 1L, "x".repeat(256)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getApiCode())
                .isEqualTo(ApiCode.INVALID_PARAM);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void listAvailableDriversShouldExcludeUnassignedDrivers() {
        ServiceFixture fixture = serviceFixture();
        Device device = device(50L, "DEV001");
        device.setEnterpriseId(10L);
        device.setFleetId(20L);

        Driver assigned = driver(40L, "D001", "assigned");
        assigned.setEnterpriseId(10L);
        assigned.setFleetId(20L);
        assigned.setStatus((byte) 1);

        Driver unassigned = driver(41L, "D002", "unassigned");
        unassigned.setEnterpriseId(10L);
        unassigned.setFleetId(null);
        unassigned.setStatus((byte) 1);

        when(fixture.driverRepository().findAll()).thenReturn(List.of(assigned, unassigned));

        var result = fixture.service().listAvailableDrivers(new DeviceAuthContext(device));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(40L);
    }

    @Test
    void signInShouldRejectUnassignedDriver() {
        ServiceFixture fixture = serviceFixture();
        Device device = device(50L, "DEV001");
        device.setEnterpriseId(10L);
        device.setFleetId(20L);

        Driver unassigned = driver(41L, "D002", "unassigned");
        unassigned.setEnterpriseId(10L);
        unassigned.setFleetId(null);
        unassigned.setStatus((byte) 1);

        when(fixture.sessionRepository().findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(50L, SessionStatus.ACTIVE.getCode()))
                .thenReturn(Optional.empty());
        when(fixture.driverRepository().findByEnterpriseIdAndDriverCode(10L, "D002"))
                .thenReturn(Optional.of(unassigned));

        assertThatThrownBy(() -> fixture.service().signIn(new DeviceAuthContext(device), new SignInSessionRequest("D002", "1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("驾驶员未分配车队");
        verify(fixture.passwordEncoder(), never()).matches(any(), any());
    }

    private DrivingSession buildSession() {
        DrivingSession session = new DrivingSession();
        session.setId(1L);
        session.setSessionNo("SES001");
        session.setEnterpriseId(10L);
        session.setFleetId(20L);
        session.setVehicleId(30L);
        session.setDriverId(40L);
        session.setDeviceId(50L);
        session.setSignInTime(LocalDateTime.of(2026, 4, 28, 9, 0, 0));
        session.setStatus(SessionStatus.ACTIVE.getCode());
        session.setUpdatedAt(LocalDateTime.of(2026, 4, 28, 9, 30, 0));
        session.setCreatedAt(LocalDateTime.of(2026, 4, 28, 8, 55, 0));
        return session;
    }

    private AuthenticatedUser operator() {
        return new AuthenticatedUser(99L, "admin", List.of("PLATFORM_SUPER_ADMIN"));
    }

    private Enterprise enterprise(Long id, String name) {
        Enterprise enterprise = new Enterprise();
        enterprise.setId(id);
        enterprise.setName(name);
        return enterprise;
    }

    private Fleet fleet(Long id, String name) {
        Fleet fleet = new Fleet();
        fleet.setId(id);
        fleet.setName(name);
        return fleet;
    }

    private Vehicle vehicle(Long id, String plateNumber) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setPlateNumber(plateNumber);
        return vehicle;
    }

    private Driver driver(Long id, String driverCode, String name) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setDriverCode(driverCode);
        driver.setName(name);
        return driver;
    }

    private Device device(Long id, String deviceCode) {
        Device device = new Device();
        device.setId(id);
        device.setDeviceCode(deviceCode);
        return device;
    }

    private ServiceFixture serviceFixture() {
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DeviceService deviceService = mock(DeviceService.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DrivingSessionService service = new DrivingSessionService(
                sessionRepository,
                driverRepository,
                deviceRepository,
                enterpriseRepository,
                fleetRepository,
                vehicleRepository,
                deviceService,
                businessAccessService,
                passwordEncoder,
                systemAuditService,
                edgeConfigVersionResolver);
        return new ServiceFixture(service, sessionRepository, driverRepository, passwordEncoder);
    }

    private record ServiceFixture(
            DrivingSessionService service,
            DrivingSessionRepository sessionRepository,
            DriverRepository driverRepository,
            PasswordEncoder passwordEncoder
    ) {
    }
}
