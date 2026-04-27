package com.example.demo.auth.repository;

import com.example.demo.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = "SELECT r.role_code FROM role r JOIN user_role ur ON ur.role_id = r.id WHERE ur.user_id = :userId", nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    List<Role> findByRoleCodeIn(List<String> roleCodes);

    List<Role> findAllByOrderByRoleCodeAsc();

    Optional<Role> findByRoleCode(String roleCode);
}
