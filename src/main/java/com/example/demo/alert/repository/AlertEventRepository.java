package com.example.demo.alert.repository;

import com.example.demo.alert.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
}
