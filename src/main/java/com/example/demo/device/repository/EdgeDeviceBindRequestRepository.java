package com.example.demo.device.repository;

import com.example.demo.device.entity.EdgeDeviceBindRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EdgeDeviceBindRequestRepository extends JpaRepository<EdgeDeviceBindRequest, Long>, JpaSpecificationExecutor<EdgeDeviceBindRequest> {
    Optional<EdgeDeviceBindRequest> findTopByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    Optional<EdgeDeviceBindRequest> findTopByDeviceIdAndStatusOrderByCreatedAtDesc(Long deviceId, String status);

    List<EdgeDeviceBindRequest> findByDeviceIdInOrderByDeviceIdAscCreatedAtDesc(Collection<Long> deviceIds);

    List<EdgeDeviceBindRequest> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiresAt);
}
