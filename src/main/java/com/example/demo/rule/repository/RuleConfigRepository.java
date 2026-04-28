package com.example.demo.rule.repository;

import com.example.demo.rule.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {
    Optional<RuleConfig> findByRuleCode(String ruleCode);

    @Query("""
            select count(r), max(r.version), max(r.publishedAt)
            from RuleConfig r
            where r.enabled = true
              and r.publishedAt is not null
              and upper(r.status) <> 'ARCHIVED'
            """)
    Object[] summarizeActiveRuleset();
}
