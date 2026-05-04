package com.example.demo.system.repository;

import com.example.demo.system.entity.SystemErrorTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SystemErrorTraceRepository extends JpaRepository<SystemErrorTrace, Long>, JpaSpecificationExecutor<SystemErrorTrace> {
    Optional<SystemErrorTrace> findFirstByTraceIdOrderByOccurredAtDescIdDesc(String traceId);
}
