package com.example.demo.auth.repository;

import com.example.demo.auth.entity.UserScopeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserScopeRoleRepository extends JpaRepository<UserScopeRole, Long> {

    List<UserScopeRole> findByUserIdAndStatusOrderByIdAsc(Long userId, Byte status);

    List<UserScopeRole> findByUserIdInAndStatusOrderByIdAsc(List<Long> userIds, Byte status);

    @Query("""
            select count(distinct ua.id)
            from UserAccount ua, UserScopeRole usr
            where ua.id = usr.userId
              and ua.subjectType = :subjectType
              and ua.status = 1
              and usr.status = :assignmentStatus
              and usr.roleCode = :roleCode
            """)
    long countEnabledUsersByRoleCode(@Param("subjectType") String subjectType,
                                     @Param("assignmentStatus") Byte assignmentStatus,
                                     @Param("roleCode") String roleCode);

    @Modifying
    @Query("delete from UserScopeRole usr where usr.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
