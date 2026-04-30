package com.example.demo.device;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.DevicePageResponseData;
import com.example.demo.device.entity.Device;
import com.example.demo.device.model.EdgeDeviceEffectiveStage;
import com.example.demo.device.model.EdgeDeviceEnterpriseBindStatus;
import com.example.demo.device.model.EdgeDeviceLifecycleStatus;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.model.EdgeDeviceSessionStage;
import com.example.demo.device.model.EdgeDeviceVehicleBindStatus;
import com.example.demo.device.repository.EdgeDeviceBindRequestHistoryRepository;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestRepository;
import com.example.demo.device.service.DeviceService;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceServiceTest {

    @Test
    void authenticateAndTouchShouldPersistDeviceOnlineAndSessionHeartbeat() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository = mock(EdgeDeviceBindRequestRepository.class);
        EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository = mock(EdgeDeviceBindRequestHistoryRepository.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DeviceService service = new DeviceService(
                deviceRepository,
                vehicleRepository,
                sessionRepository,
                enterpriseRepository,
                fleetRepository,
                driverRepository,
                edgeDeviceBindRequestRepository,
                edgeDeviceBindRequestHistoryRepository,
                businessAccessService,
                systemAuditService,
                edgeConfigVersionResolver);

        Device device = new Device();
        device.setId(1L);
        device.setDeviceCode("DEV001");
        device.setDeviceToken("TOKEN001");
        device.setStatus(EdgeDeviceStatus.BOUND.name());
        DrivingSession session = new DrivingSession();
        session.setId(2L);
        session.setDeviceId(1L);
        session.setStatus(SessionStatus.ACTIVE.getCode());

        when(deviceRepository.findByDeviceCodeAndDeviceToken("DEV001", "TOKEN001")).thenReturn(Optional.of(device));
        when(sessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(1L, SessionStatus.ACTIVE.getCode())).thenReturn(Optional.of(session));

        service.authenticateAndTouch("DEV001", "TOKEN001");

        assertThat(device.getLastSeenAt()).isNotNull();
        assertThat(session.getLastHeartbeatAt()).isEqualTo(device.getLastSeenAt());
        verify(deviceRepository).save(device);
        verify(sessionRepository).save(session);
    }

    @Test
    void listDevicesShouldIncludeCurrentDriverAndSession() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository = mock(EdgeDeviceBindRequestRepository.class);
        EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository = mock(EdgeDeviceBindRequestHistoryRepository.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DeviceService service = new DeviceService(
                deviceRepository,
                vehicleRepository,
                sessionRepository,
                enterpriseRepository,
                fleetRepository,
                driverRepository,
                edgeDeviceBindRequestRepository,
                edgeDeviceBindRequestHistoryRepository,
                businessAccessService,
                systemAuditService,
                edgeConfigVersionResolver);

        Device device = new Device();
        device.setId(1L);
        device.setEnterpriseId(10L);
        device.setFleetId(20L);
        device.setVehicleId(30L);
        device.setDeviceCode("DEV001");
        device.setDeviceName("Main Device");
        device.setStatus(EdgeDeviceStatus.BOUND.name());
        device.setLastSeenAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        DrivingSession session = new DrivingSession();
        session.setId(99L);
        session.setDeviceId(1L);
        session.setDriverId(7L);
        session.setStatus(SessionStatus.ACTIVE.getCode());
        session.setSignInTime(LocalDateTime.of(2026, 4, 28, 9, 0));

        Driver driver = new Driver();
        driver.setId(7L);
        driver.setDriverCode("DRV001");
        driver.setName("张三");

        when(businessAccessService.isSuperAdmin(any())).thenReturn(true);
        when(deviceRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(sessionRepository.findByStatusAndDeviceIdInOrderBySignInTimeDesc(SessionStatus.ACTIVE.getCode(), List.of(1L)))
                .thenReturn(List.of(session));
        when(driverRepository.findAllById(List.of(7L))).thenReturn(List.of(driver));

        DevicePageResponseData result = service.listDevices(operator(), 1, 20, null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).lastSeenAt()).isNotNull();
        assertThat(result.items().get(0).currentDriverId()).isEqualTo(7L);
        assertThat(result.items().get(0).currentDriverCode()).isEqualTo("DRV001");
        assertThat(result.items().get(0).currentSessionId()).isEqualTo(99L);
    }

    @Test
    void resolveEffectiveStageShouldFollowDocumentPriority() {
        DeviceService service = newService();

        assertThat(service.resolveEffectiveStage(
                EdgeDeviceLifecycleStatus.DISABLED,
                EdgeDeviceEnterpriseBindStatus.APPROVED,
                EdgeDeviceVehicleBindStatus.ASSIGNED,
                EdgeDeviceSessionStage.ACTIVE)).isEqualTo(EdgeDeviceEffectiveStage.DISABLED);

        assertThat(service.resolveEffectiveStage(
                EdgeDeviceLifecycleStatus.BOUND,
                EdgeDeviceEnterpriseBindStatus.APPROVED,
                EdgeDeviceVehicleBindStatus.ASSIGNED,
                EdgeDeviceSessionStage.ACTIVE)).isEqualTo(EdgeDeviceEffectiveStage.IN_SESSION);

        assertThat(service.resolveEffectiveStage(
                EdgeDeviceLifecycleStatus.BOUND,
                EdgeDeviceEnterpriseBindStatus.EXPIRED,
                EdgeDeviceVehicleBindStatus.UNASSIGNED,
                EdgeDeviceSessionStage.IDLE)).isEqualTo(EdgeDeviceEffectiveStage.CLAIM_ENTERPRISE);

        assertThat(service.resolveEffectiveStage(
                EdgeDeviceLifecycleStatus.BOUND,
                EdgeDeviceEnterpriseBindStatus.APPROVED,
                EdgeDeviceVehicleBindStatus.UNASSIGNED,
                EdgeDeviceSessionStage.IDLE)).isEqualTo(EdgeDeviceEffectiveStage.WAITING_VEHICLE);
    }

    @Test
    void reassignVehicleShouldRejectWhenVehicleAlreadyBoundByAnotherDevice() {
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository = mock(EdgeDeviceBindRequestRepository.class);
        EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository = mock(EdgeDeviceBindRequestHistoryRepository.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);
        EdgeConfigVersionResolver edgeConfigVersionResolver = mock(EdgeConfigVersionResolver.class);

        DeviceService service = new DeviceService(
                deviceRepository,
                vehicleRepository,
                sessionRepository,
                enterpriseRepository,
                fleetRepository,
                driverRepository,
                edgeDeviceBindRequestRepository,
                edgeDeviceBindRequestHistoryRepository,
                businessAccessService,
                systemAuditService,
                edgeConfigVersionResolver);

        Device device = new Device();
        device.setId(1L);
        device.setEnterpriseId(10L);
        device.setDeviceCode("DEV001");
        device.setDeviceName("Main Device");
        device.setStatus(EdgeDeviceStatus.BOUND.name());
        device.setLastActivatedAt(LocalDateTime.now());

        Vehicle vehicle = new Vehicle();
        vehicle.setId(30L);
        vehicle.setEnterpriseId(10L);
        vehicle.setFleetId(20L);
        vehicle.setPlateNumber("沪A12345");

        Device occupied = new Device();
        occupied.setId(2L);
        occupied.setVehicleId(30L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(edgeDeviceBindRequestRepository.findByDeviceIdInOrderByDeviceIdAscCreatedAtDesc(List.of(1L))).thenReturn(List.of());
        when(sessionRepository.findByStatusAndDeviceIdInOrderBySignInTimeDesc(SessionStatus.ACTIVE.getCode(), List.of(1L))).thenReturn(List.of());
        when(enterpriseRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(fleetRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(vehicleRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(vehicleRepository.findById(30L)).thenReturn(Optional.of(vehicle));
        when(deviceRepository.findByVehicleId(30L)).thenReturn(Optional.of(occupied));

        assertThatThrownBy(() -> service.reassignVehicle(operator(), 1L, new com.example.demo.device.dto.ReassignDeviceVehicleRequest(30L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getApiCode())
                .isEqualTo(ApiCode.VEHICLE_ALREADY_BOUND);
    }

    private DeviceService newService() {
        return new DeviceService(
                mock(DeviceRepository.class),
                mock(VehicleRepository.class),
                mock(DrivingSessionRepository.class),
                mock(EnterpriseRepository.class),
                mock(FleetRepository.class),
                mock(DriverRepository.class),
                mock(EdgeDeviceBindRequestRepository.class),
                mock(EdgeDeviceBindRequestHistoryRepository.class),
                mock(BusinessAccessService.class),
                mock(SystemAuditService.class),
                mock(EdgeConfigVersionResolver.class));
    }

    private AuthenticatedUser operator() {
        return new AuthenticatedUser(1L, "admin", List.of("PLATFORM_SUPER_ADMIN"));
    }
}
