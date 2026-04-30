package com.example.demo.device.repository;

import com.example.demo.device.entity.EdgeDeviceBindRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EdgeDeviceBindRequestRepository extends JpaRepository<EdgeDeviceBindRequest, Long>, JpaSpecificationExecutor<EdgeDeviceBindRequest> {
    Optional<EdgeDeviceBindRequest> findTopByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    Optional<EdgeDeviceBindRequest> findTopByDeviceIdAndStatusOrderByCreatedAtDesc(Long deviceId, String status);
}
