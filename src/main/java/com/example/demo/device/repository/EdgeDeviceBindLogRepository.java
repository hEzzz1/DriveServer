package com.example.demo.device.repository;

import com.example.demo.device.entity.EdgeDeviceBindLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EdgeDeviceBindLogRepository extends JpaRepository<EdgeDeviceBindLog, Long>, JpaSpecificationExecutor<EdgeDeviceBindLog> {
    Page<EdgeDeviceBindLog> findByEnterpriseIdOrderByCreatedAtDescIdDesc(Long enterpriseId, Pageable pageable);
}
