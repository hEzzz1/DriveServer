package com.example.demo.system.repository;

import com.example.demo.system.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemAuditRepository extends JpaRepository<SystemAuditLog, Long>, JpaSpecificationExecutor<SystemAuditLog> {
}

