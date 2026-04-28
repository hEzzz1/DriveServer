package com.example.demo.fleet.repository;

import com.example.demo.fleet.entity.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FleetRepository extends JpaRepository<Fleet, Long>, JpaSpecificationExecutor<Fleet> {
    boolean existsByEnterpriseIdAndName(Long enterpriseId, String name);

    boolean existsByEnterpriseIdAndNameAndIdNot(Long enterpriseId, String name, Long id);
}
