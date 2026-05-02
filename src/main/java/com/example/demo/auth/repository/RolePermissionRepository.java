package com.example.demo.auth.repository;

import com.example.demo.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleCodeIn(Collection<String> roleCodes);
}
