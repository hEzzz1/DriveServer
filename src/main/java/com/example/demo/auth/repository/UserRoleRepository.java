package com.example.demo.auth.repository;

import com.example.demo.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Modifying
    @Query("delete from UserRole ur where ur.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByUserIdIn(List<Long> userIds);

    long countByRoleId(Long roleId);
}
