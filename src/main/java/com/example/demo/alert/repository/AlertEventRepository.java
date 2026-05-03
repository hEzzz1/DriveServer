package com.example.demo.alert.repository;

import com.example.demo.alert.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long>, JpaSpecificationExecutor<AlertEvent> {
    long countByRuleId(Long ruleId);

    long countByRuleIdAndStatus(Long ruleId, Byte status);
}
