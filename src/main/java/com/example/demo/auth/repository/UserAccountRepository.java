package com.example.demo.auth.repository;

import com.example.demo.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByUsernameAndSubjectType(String username, String subjectType);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    @org.springframework.data.jpa.repository.Query("""
            select count(distinct ua.id)
            from UserAccount ua, UserRole ur, Role r
            where ua.id = ur.userId
              and ur.roleId = r.id
              and ua.subjectType = :subjectType
              and ua.status = 1
              and r.roleCode = :roleCode
            """)
    long countEnabledUsersByRoleCode(@org.springframework.data.repository.query.Param("subjectType") String subjectType,
                                     @org.springframework.data.repository.query.Param("roleCode") String roleCode);
}
