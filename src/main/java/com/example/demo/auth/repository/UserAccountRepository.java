package com.example.demo.auth.repository;

import com.example.demo.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByUsernameAndSubjectType(String username, String subjectType);
}
