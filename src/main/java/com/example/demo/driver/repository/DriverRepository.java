package com.example.demo.driver.repository;

import com.example.demo.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {
}
