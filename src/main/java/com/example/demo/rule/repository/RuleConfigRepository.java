package com.example.demo.rule.repository;

import com.example.demo.rule.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {
    Optional<RuleConfig> findByRuleCode(String ruleCode);
}

