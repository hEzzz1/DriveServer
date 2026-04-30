package com.example.demo.device.repository;

import com.example.demo.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    boolean existsByDeviceCode(String deviceCode);

    Optional<Device> findByDeviceCode(String deviceCode);

    Optional<Device> findByDeviceCodeAndDeviceToken(String deviceCode, String deviceToken);

    Optional<Device> findByVehicleId(Long vehicleId);

    List<Device> findByVehicleIdInOrderByStatusDescIdDesc(Collection<Long> vehicleIds);
}
