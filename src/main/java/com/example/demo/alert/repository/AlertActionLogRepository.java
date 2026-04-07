package com.example.demo.alert.repository;

import com.example.demo.alert.entity.AlertActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertActionLogRepository extends JpaRepository<AlertActionLog, Long> {
    List<AlertActionLog> findByAlertIdOrderByActionTimeAscIdAsc(Long alertId);
}
