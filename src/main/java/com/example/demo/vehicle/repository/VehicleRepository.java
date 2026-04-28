package com.example.demo.vehicle.repository;

import com.example.demo.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    boolean existsByEnterpriseIdAndPlateNumber(Long enterpriseId, String plateNumber);

    boolean existsByEnterpriseIdAndPlateNumberAndIdNot(Long enterpriseId, String plateNumber, Long id);
}
