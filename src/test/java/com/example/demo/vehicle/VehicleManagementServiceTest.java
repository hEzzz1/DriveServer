package com.example.demo.vehicle;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.dto.VehiclePageResponseData;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import com.example.demo.vehicle.service.VehicleManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleManagementServiceTest {

    @Test
    void listVehiclesShouldIncludeBoundDevice() {
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        EnterpriseRepository enterpriseRepository = mock(EnterpriseRepository.class);
        FleetRepository fleetRepository = mock(FleetRepository.class);
        BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
        SystemAuditService systemAuditService = mock(SystemAuditService.class);

        VehicleManagementService service = new VehicleManagementService(
                vehicleRepository,
                deviceRepository,
                enterpriseRepository,
                fleetRepository,
                businessAccessService,
                systemAuditService);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setEnterpriseId(10L);
        vehicle.setFleetId(20L);
        vehicle.setPlateNumber("沪A12345");
        vehicle.setStatus((byte) 1);

        Device boundDevice = new Device();
        boundDevice.setId(8L);
        boundDevice.setVehicleId(1L);
        boundDevice.setDeviceCode("DEV-8");
        boundDevice.setDeviceName("Front Camera");

        when(businessAccessService.resolveReadableEnterpriseId(any(), eq(null))).thenReturn(null);
        when(vehicleRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(deviceRepository.findByVehicleIdInOrderByStatusDescIdDesc(List.of(1L))).thenReturn(List.of(boundDevice));

        VehiclePageResponseData result = service.listVehicles(operator(), 1, 20, null, null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).boundDeviceId()).isEqualTo(8L);
        assertThat(result.items().get(0).boundDeviceCode()).isEqualTo("DEV-8");
        assertThat(result.items().get(0).boundDeviceName()).isEqualTo("Front Camera");
    }

    private AuthenticatedUser operator() {
        return new AuthenticatedUser(1L, "admin", List.of("SUPER_ADMIN"));
    }
}
