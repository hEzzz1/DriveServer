package com.example.demo.auth.repository;

import com.example.demo.auth.entity.UserScopeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserScopeRoleRepository extends JpaRepository<UserScopeRole, Long> {

    List<UserScopeRole> findByUserIdAndStatusOrderByIdAsc(Long userId, Byte status);

    @Modifying
    @Query("delete from UserScopeRole usr where usr.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
