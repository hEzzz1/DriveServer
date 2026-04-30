package com.example.demo.device.repository;

import com.example.demo.device.entity.EdgeDeviceBindLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdgeDeviceBindLogRepository extends JpaRepository<EdgeDeviceBindLog, Long> {
    Page<EdgeDeviceBindLog> findByEnterpriseIdOrderByCreatedAtDescIdDesc(Long enterpriseId, Pageable pageable);
}
