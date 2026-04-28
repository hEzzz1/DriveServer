package com.example.demo.session.repository;

import com.example.demo.session.entity.DrivingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DrivingSessionRepository extends JpaRepository<DrivingSession, Long>, JpaSpecificationExecutor<DrivingSession> {
    Optional<DrivingSession> findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(Long deviceId, Byte status);

    List<DrivingSession> findByStatusAndDeviceIdInOrderBySignInTimeDesc(Byte status, Collection<Long> deviceIds);

    List<DrivingSession> findByStatusAndDriverIdInOrderBySignInTimeDesc(Byte status, Collection<Long> driverIds);
}
