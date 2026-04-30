package com.example.demo.device.repository;

import com.example.demo.device.entity.EdgeDeviceBindRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EdgeDeviceBindRequestHistoryRepository extends JpaRepository<EdgeDeviceBindRequestHistory, Long> {
    List<EdgeDeviceBindRequestHistory> findByBindRequestIdOrderByCreatedAtAscIdAsc(Long bindRequestId);
}
